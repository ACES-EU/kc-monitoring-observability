package org.example;


import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AggregatorService {

  Connection natsConnection;
  private JetStream jetStream;

  @ConfigProperty(name = "aggregation.window.ms")
  long aggregationWindowMs;

  @ConfigProperty(name = "nats.server.url")
  String natsServerUrl;

  private static final Logger LOG = Logger.getLogger(AggregatorService.class);
  private final Map<String, TopicAggregator> topicAggregators = new ConcurrentHashMap<>();

  @PostConstruct
  void setup() throws IOException, InterruptedException, JetStreamApiException {

    Options options = new Options.Builder().server(natsServerUrl).build();
    natsConnection = Nats.connect(options);
    jetStream = natsConnection.jetStream();

    //create a stream for the aggregation
    // This is a one-time operation, so it's safe to call it every time the service starts
    StreamConfiguration streamConfig = StreamConfiguration.builder()
      .name("metrics")
      .subjects("prometheus.*", "aggregation.*")
      .storageType(StorageType.File)
      .build();

    try {
      StreamInfo streamInfo = natsConnection.jetStreamManagement().getStreamInfo("metrics");
      LOG.info("Stream 'metrics' already exists: " + streamInfo);
    } catch (Exception e) {
      LOG.info("Creating stream 'metrics'");
      natsConnection.jetStreamManagement().addStream(streamConfig);
    }

    // Subscribe to the wildcard subject to capture specific subjects
    Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {

      //if the subject is aggregation.* then ignore
      if(msg.getSubject().startsWith("aggregation.")){
        return;
      }

      String subject = msg.getSubject();
      if (!topicAggregators.containsKey(subject)) {
        topicAggregators.put(subject, new TopicAggregator(aggregationWindowMs));
        LOG.info("Created TopicAggregator for subject: " + subject);
      }

      double dataPoint = Double.parseDouble(new String(msg.getData())); // Adjust parsing as needed
      topicAggregators.get(subject).addDataPoint(dataPoint);
    });

    dispatcher.subscribe("prometheus.*");
    dispatcher.subscribe("aggregation.*");
  }

  @Scheduled(every = "10s")
  void aggregateAndPublish() {
    topicAggregators.forEach((subject, aggregator) -> {
      double average = aggregator.getAndResetAverage();
      String aggregationSubject = "aggregation." + subject;
      String message = "Average for " + subject + ": " + average;

      try {
        jetStream.publish(aggregationSubject, message.getBytes());
        LOG.info("Published to " + aggregationSubject + ": " + message);
      } catch (IOException | JetStreamApiException e) {
        LOG.error("Error publishing to " + aggregationSubject, e);
        throw new RuntimeException(e);
      }
    });
  }

  private static class TopicAggregator {
    private final long windowMs;
    private long startTime;
    private double sum = 0;
    private int count = 0;

    public TopicAggregator(long windowMs) {
      this.windowMs = windowMs;
      this.startTime = System.currentTimeMillis();
    }

    public synchronized void addDataPoint(double data) {
      sum += data;
      count++;
    }

    public synchronized double getAndResetAverage() {
      double average = (count > 0) ? (sum / count) : 0;
      reset();
      return average;
    }

    private void reset() {
      sum = 0;
      count = 0;
      startTime = System.currentTimeMillis();
    }
  }
}

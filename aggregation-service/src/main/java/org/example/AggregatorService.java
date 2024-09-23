package org.example;


import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.config.NatsConfig;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AggregatorService {

  Connection natsConnection;
  private JetStream jetStream;
  @ConfigProperty(name = "aggregation.window.ms")
  long aggregationWindowMs;
  @Inject
  NatsConfig natsConfig;

  private static final Logger LOG = Logger.getLogger(AggregatorService.class);
  private final Map<String, TopicAggregator> topicAggregators = new ConcurrentHashMap<>();

  @PostConstruct
  void setup() throws IOException, InterruptedException, JetStreamApiException {

    Options options = new Options.Builder().server(natsConfig.url()).build();
    natsConnection = Nats.connect(options);
    jetStream = natsConnection.jetStream();

    // Create a stream for the aggregation
    StreamConfiguration streamConfig = StreamConfiguration.builder()
      .name(natsConfig.stream())
      .subjects(natsConfig.inputSubjectPrefix() + ".>", natsConfig.outputSubjectPrefix() + ".>")
      .storageType(StorageType.File)
      .build();

    try {
      StreamInfo streamInfo = natsConnection.jetStreamManagement().getStreamInfo(natsConfig.stream());

      if (!streamInfo.getConfig().getSubjects().contains(natsConfig.inputSubjectPrefix() + ".>")) {
        LOG.info("Updating stream because of missing input subject " + natsConfig.stream());
        natsConnection.jetStreamManagement().updateStream(streamConfig);
      }

      if (!streamInfo.getConfig().getSubjects().contains(natsConfig.outputSubjectPrefix() + ".>")) {
        LOG.info("Updating stream because of missing output subject " + natsConfig.stream());
        natsConnection.jetStreamManagement().updateStream(streamConfig);
      }

      LOG.info("Stream " + natsConfig.stream() + " already exists");
    } catch (Exception e) {
      LOG.info("Creating stream " + natsConfig.stream());
      natsConnection.jetStreamManagement().addStream(streamConfig);
    }

    // Subscribe to the wildcard subject to capture specific subjects
    Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {

      String subject = msg.getSubject();
      if (!topicAggregators.containsKey(subject)) {
        topicAggregators.put(subject, new TopicAggregator(aggregationWindowMs));
        LOG.info("Created TopicAggregator for subject: " + subject);
      }

      topicAggregators.get(subject).addDataPoint(new BigDecimal(new String(msg.getData())));
    });

    dispatcher.subscribe(natsConfig.inputSubjectPrefix() + ".>");
  }

  @Scheduled(every = "10s")
  void aggregateAndPublish() {
    topicAggregators.forEach((subject, aggregator) -> {
      BigDecimal average = aggregator.getAndResetAverage();

      try {
        jetStream.publish(natsConfig.outputSubjectPrefix() + "." + subject, average.toString().getBytes());
        LOG.info("Published aggregated data for subject " + subject + ": " + average);
      } catch (IOException | JetStreamApiException e) {
        LOG.error("Error publishing aggregated data for " + subject, e);
        throw new RuntimeException(e);
      }
    });
  }

  private static class TopicAggregator {
    private final long windowMs;
    private long startTime;
    private BigDecimal sum = BigDecimal.ZERO;
    private int count = 0;

    public TopicAggregator(long windowMs) {
      this.windowMs = windowMs;
      this.startTime = System.currentTimeMillis();
    }

    public synchronized void addDataPoint(BigDecimal data) {
      sum = sum.add(data);
      count++;
    }

    public synchronized BigDecimal getAndResetAverage() {
      BigDecimal average = count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
      reset();
      return average;
    }

    private void reset() {
      sum = BigDecimal.ZERO;
      count = 0;
      startTime = System.currentTimeMillis();
    }
  }
}

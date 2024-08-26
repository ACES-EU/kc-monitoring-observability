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

  @ConfigProperty(name = "aggregation.result.subject")
  String resultSubject;

  @ConfigProperty(name = "nats.server.url")
  String natsServerUrl;
  private static final Logger LOG = Logger.getLogger(AggregatorService.class);
  private final Map<String, TopicAggregator> topicAggregators = new ConcurrentHashMap<>();

  @PostConstruct
  void setup() throws IOException, InterruptedException, JetStreamApiException {

    Options options = new Options.Builder().server(natsServerUrl).build();
    natsConnection = Nats.connect(options);
    jetStream = natsConnection.jetStream();

    StreamConfiguration streamConfig = StreamConfiguration.builder()
      .name("prometheus")
      .subjects("prometheus")
      .storageType(StorageType.File)
      .build();

    try {
      StreamInfo streamInfo = natsConnection.jetStreamManagement().getStreamInfo("prometheus");
      LOG.info("Stream 'prometheus' already exists: " + streamInfo);
    } catch (Exception e) {
      LOG.info("Creating stream 'prometheus'");
      natsConnection.jetStreamManagement().addStream(streamConfig);
    }
  }

  @Scheduled(every = "1s")
  void aggregateAndPublish() {
    topicAggregators.forEach((subject, aggregator) -> {
      double average = aggregator.getAndResetAverage();
      String message = "Average for " + subject + ": " + average;

      try {
        jetStream.publish(resultSubject, message.getBytes());
      } catch (IOException | JetStreamApiException e) {
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

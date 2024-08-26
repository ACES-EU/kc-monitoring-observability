package org.example;
import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@ApplicationScoped
@Path("/receive")
public class PrometheusNatsAdapter {

  @ConfigProperty(name = "nats.server.url")
  String natsServerUrl;

  private static final Logger LOG = Logger.getLogger(PrometheusNatsAdapter.class);
  private Connection natsConnection;
  private JetStream jetStream;

  @PostConstruct
  public void init() {
    try {
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

      LOG.info("Connected to NATS and initialized JetStream");
    } catch (Exception e) {
      LOG.error("Failed to initialize NATS connection and JetStream: ", e);
      throw new RuntimeException(e);
    }
  }

  @PreDestroy
  public void cleanup() {
    if (natsConnection != null) {
      try {
        natsConnection.close();
        LOG.info("Closed NATS connection");
      } catch (Exception e) {
        LOG.warn("Failed to close NATS connection: ", e);
      }
    }
  }

  @POST
  public Response receivePrometheusData(byte[] prometheusData) {
    try {
      jetStream.publish("prometheus", prometheusData);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Failed to process Prometheus data", e);
      return Response.status(500).entity("Failed to process Prometheus data").build();
    }
  }
}


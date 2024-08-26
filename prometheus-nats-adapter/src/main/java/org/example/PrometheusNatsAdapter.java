package org.example;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
@Path("/receive")
public class PrometheusNatsAdapter {

  @ConfigProperty(name = "nats.server.url")
  String natsServerUrl;

  private static final Logger LOG = Logger.getLogger(PrometheusNatsAdapter.class);

  @POST
  public Response receiveMetrics(String metrics) {
    Options options = Options.builder().server(natsServerUrl).build();
    Connection nc = null;

    LOG.info("Received metrics");
    try {
      nc = Nats.connect(options);
        nc.publish("prometheus", metrics.getBytes(StandardCharsets.UTF_8));
        LOG.info("Published metrics to NATS");
    } catch (Exception e) {
      LOG.error("An error occurred while publishing to NATS: ", e);
      return Response.serverError().entity(e.getMessage()).build();
    } finally {
      if (nc != null) {
        try {
          nc.close();
        } catch (Exception e) {
          LOG.warn("Failed to close NATS connection: ", e);
        }
      }
    }

    return Response.ok().build();
  }
}

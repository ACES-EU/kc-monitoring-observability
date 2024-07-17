package org.example.customer.service.restclient;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RestClientCaller {

    private static final Logger LOG = Logger.getLogger(RestClientCaller.class);


    @Inject
    @RestClient
    CustomerApi customerApi;

    @Scheduled(every = "5s")
    void callRestClient() {
        LOG.info("Calling rest client");
        customerApi.getAllCustomers();
    }

}

package org.example.customer.service.restclient;


import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.example.customer.service.Customer;

import java.util.List;
import java.util.concurrent.CompletionStage;
@Path("/customers")
@RegisterRestClient(configKey ="customers-api")
@RegisterClientHeaders
@Dependent
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerApi {
    @GET
    @Path("add-sample-names")
    List<Customer> addSampleNames();

    @GET
    List<Customer> getAllCustomers();
}

package org.example.customer.service;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.example.customer.service.restclient.CustomerApi;
import org.jboss.logging.Logger;


@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("customers")
@ApplicationScoped
public class CustomerResource {

    private static final Logger LOG = Logger.getLogger(CustomerResource.class);

    @Inject
    @RestClient
    CustomerApi customerApi;

    @GET
    @Path("rest-client")
    public Response getAllCustomersFromRestClient() {
        return Response.ok(customerApi.addSampleNames()).build();
    }

    @GET
    @Counted(value = "get.all.customers.counter", description = "A count of how many times getAllCustomers has been called")
    @Timed(value = "get.all.customers.timer", description = "A measure of how long it takes to get all customers")
    public Response getAllCustomers() {
        LOG.info("getAllCustomers called. Sending " + Database.getCustomersDummy().size() + " customers.");
        return Response.ok(Database.getCustomersDummy()).build();
    }

    @GET
    @Path("{customerId}")
    public Response getCustomer(@PathParam("customerId") int customerId) {
        Customer customer = Database.getCustomer(customerId);
        if (customer != null) {
            return Response.ok(customer).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("add-sample-names")
    public Response addSampleNames() {
        addNewCustomer(new Customer(Database.getCustomers().size(), "Daniel", "Ornelas"));
        addNewCustomer(new Customer(Database.getCustomers().size(), "Dennis", "McBride"));
        addNewCustomer(new Customer(Database.getCustomers().size(), "Walter", "Wright"));
        addNewCustomer(new Customer(Database.getCustomers().size(), "Mitchell", "Kish"));
        addNewCustomer(new Customer(Database.getCustomers().size(), "Tracy", "Edwards"));

        return Response.status(Response.Status.CREATED).entity(Database.getCustomers()).build();
    }

    @POST
    public Response addNewCustomer(Customer customer) {
        Database.addCustomer(customer);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{customerId}")
    public Response deleteCustomer(@PathParam("customerId") int customerId) {
        Database.deleteCustomer(customerId);
        return Response.noContent().build();
    }

}
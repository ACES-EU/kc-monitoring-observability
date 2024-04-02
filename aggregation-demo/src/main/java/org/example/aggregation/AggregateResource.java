package org.example.aggregation;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/consume")
@RequestScoped
public class AggregateResource {

    @Inject
    ConsumerRawData consumer;

    @GET
    public Response getLastFiveMessages(){

        return Response.status(200).entity(consumer.getLastFiveMessages()).build();
    }

}

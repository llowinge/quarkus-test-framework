package io.quarkus.qe.amqp;

import java.net.URI;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jms")
public class JmsResource {

    @Inject
    ConnectionFactory connectionFactory;

    @Path("/{queueName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response produceJmsQueueMessage(@PathParam("queueName") String queueName, String message) throws Exception {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            context.createProducer().send(context.createQueue(queueName), message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}

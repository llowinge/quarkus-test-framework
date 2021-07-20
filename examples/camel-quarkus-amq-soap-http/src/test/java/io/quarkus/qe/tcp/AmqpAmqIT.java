package io.quarkus.qe.tcp;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.AmqService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.AmqContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.AmqProtocol;
import io.restassured.http.ContentType;

@QuarkusScenario
public class AmqpAmqIT {

    @AmqContainer(image = "registry.redhat.io/amq7/amq-broker:latest", protocol = AmqProtocol.AMQP, expectedLog = "Server is now live")
    static final AmqService amq = new AmqService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.qpid-jms.url", amq::getAmqpUrl)
            .withProperty("quarkus.qpid-jms.username", amq::getAmqUser)
            .withProperty("quarkus.qpid-jms.password", amq::getAmqPassword);

    @Test
    public void testJmsSoapHttp() throws InterruptedException {
        final String name = "Lojza";
        final String secondName = "Lukas";
        addNameToCustomers(name);
        addNameToCustomers(secondName);
        app.logs().assertContains("Received from JMS " + name);
        app.logs().assertContains(String.format("Unmarshalled name %s", secondName));
        app.logs().assertContains(String.format("Premium customer %s", name));

    }

    private void addNameToCustomers(String name) {
        app.given()
                .contentType(ContentType.TEXT)
                .body(name)
                .post("/jms/{queueName}", "customers")
                .then()
                .statusCode(201);
    }
}

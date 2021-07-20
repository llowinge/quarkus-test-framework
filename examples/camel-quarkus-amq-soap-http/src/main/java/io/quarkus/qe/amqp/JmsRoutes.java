package io.quarkus.qe.amqp;

import io.quarkus.qe.amqp.service.GetCustomersByName;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.camel.dataformat.soap.name.TypeNameStrategy;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
public class JmsRoutes extends RouteBuilder {

    private static final String SERVICE_CUSTOMERS_BY_NAME_PACKAGE = GetCustomersByName.class.getPackage().getName();

    @ConfigProperty(name = "quarkus.http.port")
    Integer assignedPort;

    @Override
    public void configure() throws Exception {
        from("jms:queue:customers")
                .log("Received from JMS ${body}")
                .bean(JmsRoutes.class, "prepareSoapRequest")
                .marshal("soapDataFormat")
                .to("http://127.0.0.1:" + assignedPort + "/soap");

        from("platform-http:/soap")
                .log("Received from HTTP ${body}")
                .convertBodyTo(String.class)
                .choice()
                .when(xpath(
                        "/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='getCustomersByName']/name/text() = 'Lojza'"))
                .to("direct:lojza")
                .otherwise()
                .unmarshal("soapDataFormat")
                .log("Unmarshalled name ${body.getName()}")
                .end()
                .convertBodyTo(String.class);

        from("direct:lojza")
                .transform().xquery("//name/text()", String.class)
                .to("jms:queue:premiumCustomers");

        from("jms:queue:premiumCustomers")
                .log("Premium customer ${body}");
    }

    @Named("soapDataFormat")
    public SoapJaxbDataFormat soapJaxbDataFormat() {
        SoapJaxbDataFormat soapJaxbDataFormat = new SoapJaxbDataFormat(SERVICE_CUSTOMERS_BY_NAME_PACKAGE,
                new TypeNameStrategy());
        soapJaxbDataFormat.setSchema(
                "classpath:/schema/CustomerService.xsd,classpath:/soap.xsd");
        return soapJaxbDataFormat;
    }

    public GetCustomersByName prepareSoapRequest(String body) {
        GetCustomersByName request = new GetCustomersByName();
        request.setName(body);
        return request;
    }
}

package Rutas;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

@RegisterForReflection
@ApplicationScoped
public class ConsumerRoute  extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        restConfiguration()
                .bindingMode(RestBindingMode.auto);

        rest("/api").id("VerDisponibilidad")
                .get("/checkProductAvailability/{productId}/{quantity}")
                .enableCORS(true)
                .produces("application/json")
                .apiDocs(true)
                .to("direct:checkProductAvailability");

        rest("/api").id("devolverTodos")
                .get("/products")
                .enableCORS(true)
                .produces("application/json")
                .apiDocs(true)
                .to("direct:checkProducts");

    }
}

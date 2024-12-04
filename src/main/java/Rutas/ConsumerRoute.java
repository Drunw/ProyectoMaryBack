package Rutas;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

import java.awt.*;

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

        rest("/api").id("devolverOrden")
                .get("/ordenes")
                .enableCORS(true)
                .produces("application/json")
                .apiDocs(true)
                .to("direct:ordenDeCompra");

        rest("/api").id("devolverClientes")
                .get("/customers")
                .enableCORS(true)
                .produces("application/json")
                .apiDocs(true)
                .to("direct:checkClientes");
      
        rest("/send-pdf")
                .post()
                .enableCORS(true)
                .consumes(MediaType.MULTIPART_FORM_DATA)
                .to("direct:sendEmail");

        from("timer://myTimer?period=30000")  // 30000 ms = 30 segundos
                .log("Ejecutando tarea programada cada 30 segundos");

        rest("/api").id("AgregarCliente")
                .get("/addClient/{customerName}/{customerPhone}/{idType}/{customeId}/{addres}/{city}")
                .enableCORS(true)
                .produces("application/json")
                .apiDocs(true)
                .to("direct:agregarClientes");
    }
}

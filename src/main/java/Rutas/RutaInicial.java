package Rutas;

import config.LoggerAuditoriaFuse;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.util.ByteArrayDataSource;
import model.ProductAvailabilityResponse;
import model.ResponseOrden;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.InputStream;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RegisterForReflection
@ApplicationScoped
public class RutaInicial extends RouteBuilder {

    @Inject
    LoggerAuditoriaFuse loggerAuditoriaFuse;

    final String fixedEmail = "distribucionespremiumcial@gmail.com";

    @Override
    public void configure() throws Exception {

        from("direct:checkProductAvailability").routeId("Ruta inicial")
                .setProperty("productId",simple("${headers.productId}"))
                .setProperty("quantity",simple("${headers.quantity}"))
                .log("Comienza busqueda•") 
                .to("sql:SELECT id, name, price, units FROM productos WHERE id = CAST(:#productId AS INTEGER)")
                .log("Termino busqueda.")
                .process(exchange -> {
                    ArrayList<LinkedCaseInsensitiveMap<String>> parametros = (ArrayList<LinkedCaseInsensitiveMap<String>>) exchange.getIn().getBody();
                    Integer parametro = Integer.valueOf(String.valueOf(parametros.get(0).get("units")));
                    Integer entrada = Integer.valueOf(String.valueOf(exchange.getProperty("quantity")));
                    String name = String.valueOf(parametros.get(0).get("name"));
                    Double price = Double.valueOf(String.valueOf(parametros.get(0).get("price")));
                    if (parametro <= 0) {
                        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                    } else if (parametro < entrada) {
                        name = "no";
                        price = 0.0;
                        exchange.setProperty("bodyFinal", new ProductAvailabilityResponse(name, price));
                    } else {
                        exchange.setProperty("bodyFinal", new ProductAvailabilityResponse(name, price));
                        Integer cantidadFinal = parametro - entrada;
                        exchange.setProperty("conicion","1");
                        exchange.setProperty("cantidadFinal",cantidadFinal);
                    }
                })
                .choice().when(simple("${exchangeProperty.conicion} == '1'"))
                .setHeader("cantidad",simple("${exchangeProperty.cantidadFinal}"))
                .toD("sql:UPDATE productos set units = CAST(:#cantidad AS INTEGER) WHERE id = CAST(:#productId AS INTEGER)")
                .to("direct:ordenDeCompra")
                .process(exchange -> {
                    Integer numero = (Integer) exchange.getIn().getHeader("numero")+1;
                    exchange.getIn().setHeader("numero",numero);
                })
                .to("sql:UPDATE ordenescompra set numero = CAST(:#numero AS INTEGER) WHERE id = '1'")
                .end()
                .setBody(simple("${exchangeProperty.bodyFinal}"))
                .end();

        from("direct:checkProducts").routeId("ObtenerTodos")
                .to("sql:SELECT * FROM productos")
                .end();
      
        from("direct:ordenDeCompra").routeId("ordenDeCompra")
                .to("sql:SELECT numero FROM ordenescompra WHERE id = '1'")
                .process(exchange -> {
                    ArrayList<LinkedCaseInsensitiveMap<String>> parametros = (ArrayList<LinkedCaseInsensitiveMap<String>>) exchange.getIn().getBody();
                    Integer numero = Integer.valueOf(parametros.get(0).get("numero"));
                    exchange.getIn().setBody(new ResponseOrden(numero));
                    exchange.getIn().setHeader("numero",numero);
                    }
                )
                .end();
      
        from("direct:sendEmail")
                .process(exchange -> {
                    byte[] fileBytes = exchange.getIn().getBody(byte[].class); // Obtener el PDF como BLOB (byte[])
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String timestamp = now.format(formatter);  // Formatear la fecha y hora
                    String subject = "ORDEN DE COMPRA GENERADA - " + timestamp;
                    String to = "distribucionespremiumcial@gmail.com"; // Dirección de correo del destinatario

                    exchange.getIn().setHeader("Subject", subject);
                    exchange.getIn().setHeader("To", to);
                    exchange.getIn().setHeader("CamelFileName", "documento.pdf");
                    exchange.getIn().setBody(fileBytes); // El archivo PDF en el cuerpo del correo
                    exchange.getIn().setHeader("Content-Type", "application/pdf"); // Tipo de contenido para el archivo adjunto
                })
                .to("smtp://smtp.gmail.com:587?username=distribucionespremiumcial@gmail.com&password=fkfa%20yjkz%20bbaq%20rreq&from=distribucionespremiumcial@gmail.com&to=distribucionespremiumcial@gmail.com&subject=Prueba1&mail.smtp.auth=true&mail.smtp.starttls.enable=true");
    }
}


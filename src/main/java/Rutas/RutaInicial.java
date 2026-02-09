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
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.attachment.AttachmentMessage;

import jakarta.activation.DataHandler;
import java.io.InputStream;

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
                .log("Comienza busqueda")
                .to("sql:SELECT id, name, price, units FROM productos WHERE id = CAST(:#productId AS INTEGER)")
                .log("termina busqueda.")
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
                .end()
                .setBody(simple("${exchangeProperty.bodyFinal}"))
                .end();

        from("direct:checkProducts").routeId("ObtenerTodos")
                .log("Comienza busqueda productos.")
                .to("sql:SELECT * FROM productos")
                .log("ejecutado")
                .end();

        from("direct:checkClientes").routeId("ObtenerTodosClientes")
                .log("Comienza busqueda clientes.")
                .to("sql:SELECT * FROM clientes")
                .log("ejecutado clientes")
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

        // 1) Obtener PDF como bytes (tu forma) + fallback por attachments (por si el multipart no llega directo)
          byte[] fileBytes = exchange.getMessage().getBody(byte[].class);

          if (fileBytes == null || fileBytes.length == 0) {
              AttachmentMessage am = exchange.getMessage(AttachmentMessage.class);

              // Si sabes el nombre del campo del multipart (ej: "file"), mejor:
              Attachment att = am.getAttachmentObject("file");

              // Fallback: toma el primero que exista
              if (att == null) {
                  att = am.getAttachmentObjects().values().stream()
                          .findFirst()
                          .orElseThrow(() -> new IllegalStateException(
                                  "No llegó archivo PDF en el multipart (attachments vacío)"
                          ));
              }

              DataHandler dh = att.getDataHandler();
              try (InputStream is = dh.getInputStream()) {
                  fileBytes = is.readAllBytes();
              }
          }
        // 2) Subject con hora Bogotá (como ya lo venías haciendo)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime adjustedTime = now.minusHours(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = adjustedTime.format(formatter);
        String subject = "ORDEN DE COMPRA GENERADA - " + timestamp;
        String to = "distribucionespremiumcial@gmail.com";
        // 3) Base64 del PDF para Apps Script
        String pdfB64 = Base64.getEncoder().encodeToString(fileBytes);
        // 4) Armar JSON payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", System.getenv("tokenOla"));
        payload.put("to", to);
        payload.put("subject", subject);
        payload.put("text", "Adjunto encontrarás la orden de compra en PDF.");

        Map<String, Object> att = new HashMap<>();
        att.put("filename", "documento.pdf");
        att.put("mimeType", "application/pdf");
        att.put("contentBase64", pdfB64);

        payload.put("attachments", List.of(att));

        exchange.getIn().setBody(payload);
      })
      // 5) JSON + POST al Apps Script Web App
      .marshal().json()
      .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
      .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .log("Antes de enviar")
                .toD("https://script.google.com/macros/s/AKfycbwZRcT_t1v3XTKqbYv1WCya8tYh1NO8rd1KMqBSPsGxRuAjftQmW_oj-RfoOryGXueUSg/exec?bridgeEndpoint=true&throwExceptionOnFailure=false&httpMethod=POST")
                .setProperty("gasCode", header(Exchange.HTTP_RESPONSE_CODE))
                .setProperty("gasLocation", header("Location"))
                .choice()
                .when(exchangeProperty("gasCode").isEqualTo(302))
                .log("en el get")
                // Ir a la URL Location para ver el JSON final
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .removeHeader(Exchange.CONTENT_TYPE)
                .toD("${exchangeProperty.gasLocation}&bridgeEndpoint=true&throwExceptionOnFailure=true")
                .end()

                .log("GAS final code=${header.CamelHttpResponseCode} body=${body}")
      .setProperty("bodyResponse", simple("${body}"))
      // 7) Tu flujo igual
      .to("direct:ordenDeCompra")
      .process(exchange -> {
        Integer numero = (Integer) exchange.getIn().getHeader("numero") + 1;
        exchange.getIn().setHeader("numero", numero);
      })
      .to("sql:UPDATE ordenescompra set numero = CAST(:#numero AS INTEGER) WHERE id = '1'")
      .setBody(simple("${exchangeProperty.bodyResponse}"));

        from("direct:agregarClientes").routeId("AgregarClientes")
                .log("Headers: ${headers}")
                .setProperty("name",simple("${headers.customerName}"))
                .setProperty("phone",simple("${headers.customerPhone}"))
                .setProperty("idType",simple("${headers.idType}"))
                .setProperty("customeId",simple("${headers.customeId}"))
                .setProperty("addres",simple("${headers.addres}"))
                .setProperty("city",simple("${headers.city}"))
                .to("sql: INSERT INTO clientes (name , phone ,idtype, idnumber, address, city) VALUES (:#customerName,:#customerPhone,:#idType, :#customeId, :#addres, :#city)")
                .end();

        from("direct:actualizarClientes").routeId("ActualizarClientes")
                .to("sql: UPDATE clientes SET name = :#customerName , phone = :#customerPhone , idtype = :#idType , idnumber = :#customeId , address = :#addres , city = :#city WHERE id = CAST(:#id AS INTEGER)")
                .end();
    }
}


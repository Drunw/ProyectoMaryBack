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

        // La llamada a Apps Script (POST + redirecciones hasta el JSON final) es una sola
        // operación atómica; si falla (red, timeout, statusCode fuera de 2xx) reintentamos
        // TODA la secuencia, porque cada intento genera un link de redirección nuevo.
        onException(java.io.IOException.class, org.apache.camel.http.base.HttpOperationFailedException.class)
                .maximumRedeliveries(2)
                .redeliveryDelay(1500)
                .logExhausted(true)
                .handled(false);

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
      // 5) POST al Apps Script Web App con java.net.http.HttpClient: Google responde con
      //    un 302 hacia script.googleusercontent.com (otro dominio) y ahí, típicamente,
      //    otro salto más antes del JSON final. camel-http (tras el upgrade a Camel 4)
      //    dejó de seguir esa cadena de forma confiable, así que la resolvemos con un
      //    cliente que sí sigue redirecciones (incluida la conversión POST->GET) en una
      //    sola llamada atómica: si falla, el reintento de onException repite TODO
      //    (POST + redirecciones), no un link ya muerto.
      .log("Antes de enviar")
      .process(exchange -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) exchange.getIn().getBody();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonBody = mapper.writeValueAsString(payload);

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://script.google.com/macros/s/AKfycbwZRcT_t1v3XTKqbYv1WCya8tYh1NO8rd1KMqBSPsGxRuAjftQmW_oj-RfoOryGXueUSg/exec"))
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new java.io.IOException("GAS respondió con statusCode=" + response.statusCode() + " body=" + response.body());
        }

        exchange.setProperty("gasCode", response.statusCode());
        exchange.getIn().setBody(response.body());
      })
      .log("GAS final code=${exchangeProperty.gasCode} body=${body}")
      .setProperty("bodyResponse", simple("${body}"))
      // 7) Tu flujo igual (consulta el número de orden en línea, sin saltar a
      //    direct:ordenDeCompra: Camel fusiona esa ruta con la REST /api/ordenes
      //    cuando solo tiene un consumidor, y deja de existir como endpoint aparte)
      .to("sql:SELECT numero FROM ordenescompra WHERE id = '1'")
      .process(exchange -> {
        ArrayList<LinkedCaseInsensitiveMap<String>> parametros = (ArrayList<LinkedCaseInsensitiveMap<String>>) exchange.getIn().getBody();
        Integer numero = Integer.valueOf(parametros.get(0).get("numero"));
        exchange.getIn().setHeader("numero", numero);
      })
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


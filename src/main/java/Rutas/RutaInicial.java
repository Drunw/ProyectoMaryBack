package Rutas;

import config.LoggerAuditoriaFuse;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import model.ProductAvailabilityResponse;
import model.ResponseOrden;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

@RegisterForReflection
@ApplicationScoped
public class RutaInicial extends RouteBuilder {

    @Inject
    LoggerAuditoriaFuse loggerAuditoriaFuse;

    @Override
    public void configure() throws Exception {

        from("direct:checkProductAvailability").routeId("Ruta inicial")
                .setProperty("productId",simple("${headers.productId}"))
                .setProperty("quantity",simple("${headers.quantity}"))
                .to("sql:SELECT id, name, price, units FROM productos WHERE id = CAST(:#productId AS INTEGER)")
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
                        exchange.getIn().setBody(new ProductAvailabilityResponse(name, price));
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
    }
}

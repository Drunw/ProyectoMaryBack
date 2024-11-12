package config;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RegisterForReflection
@ApplicationScoped
public class LoggerAuditoriaFuse {

    /**
     * NOMBRES METODOS CREADOS
     */

    public static final String LOG_PETICION_ENTRADA = "imprimirLogEntradaAPI(${exchangeProperty.idTransaccion},${routeId},${body})";
    public static final String LOG_PETICION_SALIDA = "imprimirLogSalidaAPI(${exchangeProperty.idTransaccion},${routeId},${body})";
    public static final String LOG_PETICION_SALIDA_ERROR =  "imprimirLogSalidaAPI(${exchangeProperty.idTransaccion},${routeId})";
    public static final String LOG_PETICION_ENDPOINT = "imprimirLogPeticionEndpoint(${exchangeProperty.idTransaccion},${routeId},${body},${exchangeProperty.endpointConsumo})";
    public static final String LOG_PETICION_ENDPOINT_HIJO = "imprimirLogPeticionEndpointHijo(${exchangeProperty.idTransaccion}, ${exchangeProperty.idTransaccionHijo},${routeId},${body},${exchangeProperty.endpointConsumo})";

    public static final String LOG_RESPUESTA_ENDPOINT = "imprimirLogRespuestaEndpoint(${exchangeProperty.idTransaccion},${routeId},${body},${exchangeProperty.endpointConsumo},${header.CamelHttpResponseCode})";
    public static final String LOG_RESPUESTA_ENDPOINT_HIJO = "imprimirLogRespuestaEndpointHijo(${exchangeProperty.idTransaccion},${exchangeProperty.idTransaccionHijo},${routeId},${body},${exchangeProperty.endpointConsumo},${header.CamelHttpResponseCode})";

    public static final String LOG_PETICION_PROCEDIMIENTO = "imprimirLogPeticionProcedimeintoAlmacenado(${exchangeProperty.idTransaccion},${routeId}, ${body},${exchangeProperty.procedimiento})";
    public static final String LOG_PETICION_PROCEDIMIENTO_HIJO = "imprimirLogPeticionProcedimeintoAlmacenadoHijo(${exchangeProperty.idTransaccion},${exchangeProperty.idTransaccionHijo},${routeId}, ${body},${exchangeProperty.procedimiento})";

    public static final String LOG_PRESPUESTA_PROCEDIMIENTO = "imprimirLogRespuestaProcedimeintoAlmacenado(${exchangeProperty.idTransaccion},${routeId}, ${body},${exchangeProperty.procedimiento} )";
    public static final String LOG_RESPUESTA_PROCEDIMIENTO_HIJO = "imprimirLogRespuestaProcedimeintoAlmacenadoHijo(${exchangeProperty.idTransaccion},${exchangeProperty.idTransaccionHijo},${routeId}, ${body},${exchangeProperty.procedimiento} )";

    public static final String LOG_ERROR = "imprimirLogError(${exchangeProperty.idTransaccion},${routeId},${exchangeProperty.message})";
    public static final String LOG_SIMPLE = "imprimirLogSimple(${exchangeProperty.idTransaccion},${routeId},${exchangeProperty.message})";

    public static final String LOG_SIMPLE_HIJO = "imprimirLogSimpleHijo(${exchangeProperty.idTransaccion},${exchangeProperty.idTransaccionHijo},${routeId},${exchangeProperty.message})";

    /**
     * NOMBRES PROPIEDADES NECESARIAS PARA AUDITORIA
     */
    public static final String PROPIEDAD_TRANSACCION = "idTransaccion";
    public static final String PROPIEDAD_TRANSACCION_HIJO = "idTransaccionHijo";
    public static final String PROPIEDAD_ENDPOINT= "endpointConsumo";
    public static final String PROPIEDAD_PROCEDIMIENTO = "procedimiento";
    public static final String PROPIEDAD_MENSAJE = "message";


    private static final Logger logger = LoggerFactory.getLogger(LoggerAuditoriaFuse.class);

    /**
     * Metodo para escribir log de entrada de API
     * @param idTransaccion
     * @param idRuta
     * @param body
     */
    public void imprimirLogEntradaAPI(String idTransaccion, String idRuta, String body) {
        String mensaje = "Peticion de entrada al API ";
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\",\"entrada\":\"%S\"}",
                idRuta, idTransaccion, mensaje, body);
        logger.info(log);
    }

    /**
     * Metodo para escribir Log de salida del api
     * @param idTransaccion
     * @param idRuta
     * @param body
     */
    public void imprimirLogSalidaAPI(String idTransaccion, String idRuta, String body) {
        String mensaje = "Body de salida del API  ";
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\" ,\"salida\":\"%S\"}",
                idRuta, idTransaccion, mensaje, body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de peticion a un endpoint externo
     * @param idTransaccion
     * @param idRuta
     * @param body
     * @param endpointConsumo
     */
    public void imprimirLogPeticionEndpoint(String idTransaccion, String idRuta, String body, String endpointConsumo) {
        String mensaje = "Peticion de consumo a endpoint ".concat(endpointConsumo);
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\",\"endpoint\":\"%S\",\"peticion\":\"%S\"}",
                idRuta, idTransaccion, mensaje, endpointConsumo.toLowerCase(), body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de peticion a un endpoint externo durante una iteracion
     * haciendo uso de un id por transaccion y un id por peticion
     * @param idTransaccion
     * @param idTransaccionHijo
     * @param idRuta
     * @param body
     * @param endpointConsumo
     */
    public void imprimirLogPeticionEndpointHijo(String idTransaccion, String idTransaccionHijo, String idRuta, String body, String endpointConsumo) {
        String mensaje = "Peticion de consumo a endpoint ".concat(endpointConsumo);
        String log = String.format("{\"route\":\"%S\", \"traceIdPrincial\":\"%S\", \"traceIdSecundario\":\"%S\"" +
                        ", \"message\":\"%s\",\"endpoint\":\"%S\",\"peticion\":\"%S\"}",
                idRuta, idTransaccion, idTransaccionHijo, mensaje, endpointConsumo.toLowerCase(), body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir en log la respuesta obtenida al conumir un endpoint externo
     * @param idTransaccion
     * @param idRuta
     * @param body
     * @param endpointConsumo
     * @param codigoRespuesta
     */
    public void imprimirLogRespuestaEndpoint(String idTransaccion, String idRuta,
                                             String body, String endpointConsumo, String codigoRespuesta) {
        String mensaje = "Respuesta consumo a endpoint ".concat(endpointConsumo);
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\",\"endpoint\":\"%S\"" +
                        ",\"respuesta\":\"%S\",\"codigoRespuesta\":\"%S\"}",
                idRuta, idTransaccion, mensaje, endpointConsumo.toLowerCase(), body, codigoRespuesta);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de respuesta obtenida al consumir un endpoint externo durante una iteracion
     * haciendo uso de un id por transaccion y un id por peticion
     * @param idTransaccion
     * @param idTransaccionHijo
     * @param idRuta
     * @param body
     * @param endpointConsumo
     * @param codigoRespuesta
     */
    public void imprimirLogRespuestaEndpointHijo(String idTransaccion, String idTransaccionHijo, String idRuta,
                                                 String body, String endpointConsumo, String codigoRespuesta) {
        String mensaje = "Respuesta consumo a endpoint ".concat(endpointConsumo);
        String log = String.format("{\"route\":\"%S\",\"traceIdPrincial\":\"%S\", \"traceIdSecundario\":\"%S\"" +
                        ", \"message\":\"%s\",\"endpoint\":\"%S\"" +
                        ",\"respuesta\":\"%S\",\"codigoRespuesta\":\"%S\"}",
                idRuta, idTransaccion, idTransaccionHijo, mensaje, endpointConsumo.toLowerCase(), body, codigoRespuesta);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de solicitud de consumo a un procedimiento almacenado
     * @param idTransaccion
     * @param idRuta
     * @param body
     * @param procedimientoConsumo
     */
    public void imprimirLogPeticionProcedimeintoAlmacenado(String idTransaccion,
                                                           String idRuta, String body, String procedimientoConsumo) {
        String mensaje = "Peticion consumo procedimiento almacenado ".concat(procedimientoConsumo);
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\",\"procedimeinto\":\"%S\",\"peticion\":\"%S\"}",
                idRuta, idTransaccion, mensaje, procedimientoConsumo, body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de solicitud de consumo a un procedimiento almacenado durante una iteracion
     * haciendo uso de un id por transaccion y un id por peticion
     * @param idTransaccion
     * @param idTransaccionHijo
     * @param idRuta
     * @param body
     * @param procedimientoConsumo
     */
    public void imprimirLogPeticionProcedimeintoAlmacenadoHijo(String idTransaccion,String idTransaccionHijo,
                                                               String idRuta, String body, String procedimientoConsumo) {
        String mensaje = "Peticion consumo procedimiento almacenado ".concat(procedimientoConsumo);
        String log = String.format("{\"route\":\"%S\",\"traceIdPrincial\":\"%S\", \"traceIdSecundario\":\"%S\"" +
                        ", \"message\":\"%s\",\"procedimeinto\":\"%S\",\"peticion\":\"%S\"}",
                idRuta, idTransaccion, idTransaccionHijo,mensaje, procedimientoConsumo, body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de respuesta de consumo a procedimiento almacenado
     * @param idTransaccion
     * @param idRuta
     * @param body
     * @param procedimientoConsumo
     */
    public void imprimirLogRespuestaProcedimeintoAlmacenado(String idTransaccion,
                                                            String idRuta, String body, String procedimientoConsumo) {
        String mensaje = "Respuesta consumo procedimiento almacenado ".concat(procedimientoConsumo);
        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"message\":\"%s\",\"procedimeinto\":\"%S\",\"respuesta\":\"%S\"}",
                idRuta, idTransaccion, mensaje, procedimientoConsumo, body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de respuesta de consumo a procedimiento almacenado durante una iteracion
     * haciendo uso de un id por transaccion y un id por peticion
     * @param idTransaccion
     * @param idRuta
     * @param body
     * @param procedimientoConsumo
     */
    public void imprimirLogRespuestaProcedimeintoAlmacenadoHijo(String idTransaccion,String idTransaccionHijo,
                                                                String idRuta, String body, String procedimientoConsumo) {
        String mensaje = "Respuesta consumo procedimiento almacenado ".concat(procedimientoConsumo);
        String log = String.format("{\"route\":\"%S\",\"traceIdPrincial\":\"%S\", \"traceIdSecundario\":\"%S\", \"message\":\"%s\",\"procedimeinto\":\"%S\",\"respuesta\":\"%S\"}",
                idRuta, idTransaccion, idTransaccionHijo,mensaje, procedimientoConsumo, body);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de error
     * @param idTransaccion
     * @param idRuta
     * @param mensaje
     */
    public void imprimirLogError(String idTransaccion, String idRuta, String mensaje) {

        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"error\":\"%s\"}",
                idRuta, idTransaccion, mensaje);
        logger.error(log);
    }

    /**
     * Metodo para imprimir log de informacion simple
     * @param idTransaccion
     * @param idRuta
     * @param mensaje
     */
    public void imprimirLogSimple(String idTransaccion, String idRuta, String mensaje) {

        String log = String.format("{\"route\":\"%S\", \"traceId\":\"%S\", \"mensaje\":\"%s\"}",
                idRuta, idTransaccion, mensaje);
        logger.info(log);
    }

    /**
     * Metodo para imprimir log de informacion simple Hijo
     * @param idTransaccion
     * @param idRuta
     * @param mensaje
     */
    public void imprimirLogSimpleHijo(String idTransaccion,String idTransaccionHijo, String idRuta, String mensaje) {

        String log = String.format("{\"route\":\"%S\", \"traceIdPrincial\":\"%S\", \"traceIdSecundario\":\"%S\", \"mensaje\":\"%s\"}",
                idRuta, idTransaccion, idTransaccionHijo, mensaje);
        logger.info(log);
    }
}


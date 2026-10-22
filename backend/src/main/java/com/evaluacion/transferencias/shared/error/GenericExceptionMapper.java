package com.evaluacion.transferencias.shared.error;

import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (esJsonInvalido(exception)) {
            return respuesta(400, "JSON_INVALIDO", "El cuerpo de la solicitud no es un JSON válido");
        }
        if (exception instanceof WebApplicationException web) {
            int status = web.getResponse().getStatus();
            return respuesta(status, codigoHttp(status), web.getMessage());
        }
        LOG.error("error.inesperado", exception);
        return respuesta(500, "ERROR_INTERNO", "Ocurrió un error inesperado. Intente nuevamente.");
    }

    private static boolean esJsonInvalido(Throwable exception) {
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof JsonProcessingException) {
                return true;
            }
        }
        return false;
    }

    private static String codigoHttp(int status) {
        return switch (status) {
            case 400 -> "SOLICITUD_INVALIDA";
            case 404 -> "RECURSO_NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "TIPO_NO_SOPORTADO";
            default -> "ERROR_HTTP";
        };
    }

    private static Response respuesta(int status, String codigo, String mensaje) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(status, codigo, mensaje, List.of()))
                .build();
    }
}

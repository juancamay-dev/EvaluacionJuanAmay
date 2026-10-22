package com.evaluacion.transferencias.shared.error;

import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidacionExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> detalles = exception.getConstraintViolations().stream()
                .map(v -> nombreDelCampo(v) + ": " + v.getMessage())
                .sorted()
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(400, "VALIDACION", "La solicitud contiene datos inválidos", detalles))
                .build();
    }

    /** "crear.request.monto" -> "monto": el cliente solo conoce el nombre del campo. */
    private static String nombreDelCampo(ConstraintViolation<?> violacion) {
        String nombre = null;
        for (Path.Node nodo : violacion.getPropertyPath()) {
            nombre = nodo.getName();
        }
        return nombre;
    }
}

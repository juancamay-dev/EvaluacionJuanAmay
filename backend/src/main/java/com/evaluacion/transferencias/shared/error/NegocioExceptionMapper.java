package com.evaluacion.transferencias.shared.error;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NegocioExceptionMapper implements ExceptionMapper<NegocioException> {

    @Override
    public Response toResponse(NegocioException exception) {
        List<String> detalles = exception instanceof TransferenciaRechazadaException rechazo
                && rechazo.getTransferenciaId() != null
                ? List.of("transferenciaId: " + rechazo.getTransferenciaId())
                : List.of();
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(exception.getStatus(), exception.getCodigo(), exception.getMessage(),
                        detalles))
                .build();
    }
}

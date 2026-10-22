package com.evaluacion.transferencias.shared.error;

import java.time.Instant;
import java.util.List;

/** Formato único de error de la API. */
public record ErrorResponse(Instant timestamp, int status, String codigo, String mensaje, List<String> detalles) {

    public static ErrorResponse of(int status, String codigo, String mensaje, List<String> detalles) {
        return new ErrorResponse(Instant.now(), status, codigo, mensaje, List.copyOf(detalles));
    }
}

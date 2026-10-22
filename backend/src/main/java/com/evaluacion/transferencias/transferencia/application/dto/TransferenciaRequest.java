package com.evaluacion.transferencias.transferencia.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TransferenciaRequest(
        @NotNull(message = "es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        String cuentaOrigen,

        @NotNull(message = "es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        String cuentaDestino,

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "admite máximo 13 enteros y 2 decimales")
        BigDecimal monto) {
}

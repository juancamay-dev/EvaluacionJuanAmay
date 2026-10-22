package com.evaluacion.transferencias.transferencia.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record TransferenciaRequest(
        @NotNull(message = "es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        @Schema(description = "Número de la cuenta que envía", example = "1000000001")
        String cuentaOrigen,

        @NotNull(message = "es obligatoria")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        @Schema(description = "Número de la cuenta que recibe", example = "1000000002")
        String cuentaDestino,

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "admite máximo 13 enteros y 2 decimales")
        @Schema(description = "Monto en dólares, mayor a cero y con máximo 2 decimales", example = "250.00")
        BigDecimal monto) {
}

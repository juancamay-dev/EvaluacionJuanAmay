package com.evaluacion.transferencias.cuenta.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;

public record CuentaRequest(
        @NotNull(message = "es obligatorio")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        @Schema(description = "Número único de 10 dígitos", example = "1000000010")
        String numeroCuenta,

        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        @Schema(description = "Nombre del titular", example = "Ana Pérez")
        String titular,

        @NotNull(message = "es obligatorio (AHORRO o CORRIENTE)")
        @Schema(description = "AHORRO o CORRIENTE", example = "AHORRO")
        TipoCuenta tipo,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.00", message = "no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "admite máximo 13 enteros y 2 decimales")
        @Schema(description = "Saldo inicial, mayor o igual a cero", example = "1500.00")
        BigDecimal saldoInicial,

        @NotNull(message = "es obligatorio (ACTIVA o INACTIVA)")
        @Schema(description = "ACTIVA o INACTIVA", example = "ACTIVA")
        EstadoCuenta estado) {
}

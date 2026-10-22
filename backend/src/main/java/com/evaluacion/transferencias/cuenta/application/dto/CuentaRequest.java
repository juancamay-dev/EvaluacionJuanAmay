package com.evaluacion.transferencias.cuenta.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;

public record CuentaRequest(
        @NotNull(message = "es obligatorio")
        @Pattern(regexp = "\\d{10}", message = "debe tener exactamente 10 dígitos")
        String numeroCuenta,

        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String titular,

        @NotNull(message = "es obligatorio (AHORRO o CORRIENTE)")
        TipoCuenta tipo,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.00", message = "no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "admite máximo 13 enteros y 2 decimales")
        BigDecimal saldoInicial,

        @NotNull(message = "es obligatorio (ACTIVA o INACTIVA)")
        EstadoCuenta estado) {
}

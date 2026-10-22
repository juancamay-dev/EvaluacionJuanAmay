package com.evaluacion.transferencias.cuenta.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;

public record CuentaResponse(Long id, String numeroCuenta, String titular, TipoCuenta tipo, BigDecimal saldo,
                             EstadoCuenta estado, Instant fechaCreacion) {
}

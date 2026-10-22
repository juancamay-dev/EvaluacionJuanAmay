package com.evaluacion.transferencias.cuenta.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumenCuentaResponse(Long cuentaId, String numeroCuenta, String titular, LocalDate fecha,
                                    BigDecimal saldoActual, BigDecimal totalTransferidoHoy,
                                    long cantidadTransferenciasHoy, long sospechosasHoy, long sospechosasTotales,
                                    BigDecimal limiteDiario, BigDecimal disponibleHoy) {
}

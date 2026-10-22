package com.evaluacion.transferencias.transferencia.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;

public record TransferenciaResponse(Long id, String cuentaOrigen, String cuentaDestino, BigDecimal monto,
                                    EstadoTransferencia estado, String motivo, Instant fecha) {
}

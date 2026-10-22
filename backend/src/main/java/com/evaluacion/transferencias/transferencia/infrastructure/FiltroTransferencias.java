package com.evaluacion.transferencias.transferencia.infrastructure;

import java.time.Instant;

import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;

/** Criterios opcionales del historial; las fechas forman el intervalo [desde, hasta). */
public record FiltroTransferencias(String cuentaOrigen, String cuentaDestino, Instant desde, Instant hasta,
                                   EstadoTransferencia estado) {
}

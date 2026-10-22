package com.evaluacion.transferencias.transferencia.domain;

import java.math.BigDecimal;

/** Resultado de comparar un monto con lo que queda del límite diario. */
public record EvaluacionLimite(BigDecimal limite, BigDecimal acumulado, BigDecimal disponible, boolean excedido) {
}

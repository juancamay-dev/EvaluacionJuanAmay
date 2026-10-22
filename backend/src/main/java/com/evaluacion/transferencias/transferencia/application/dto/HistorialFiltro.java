package com.evaluacion.transferencias.transferencia.application.dto;

/**
 * Filtros opcionales del historial tal como llegan de la API: fechas locales inclusivas
 * (yyyy-MM-dd) y estado como texto; el servicio los valida y convierte.
 */
public record HistorialFiltro(String cuentaOrigen, String cuentaDestino, String desde, String hasta,
                              String estado) {
}

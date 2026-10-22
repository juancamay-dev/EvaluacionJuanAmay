package com.evaluacion.transferencias.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Día calendario de un instante en la zona del negocio, expresado como el
 * intervalo semiabierto [inicio, fin) en UTC.
 */
public record RangoDia(LocalDate fecha, Instant inicio, Instant fin) {

    public static RangoDia de(Instant instante, ZoneId zona) {
        LocalDate fecha = LocalDate.ofInstant(instante, zona);
        return new RangoDia(fecha, fecha.atStartOfDay(zona).toInstant(),
                fecha.plusDays(1).atStartOfDay(zona).toInstant());
    }
}

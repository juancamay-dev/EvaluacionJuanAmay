package com.evaluacion.transferencias.shared.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RangoDiaTest {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    @Test
    void inicioYFinCorrespondenAMedianocheDeEcuador() {
        RangoDia rango = RangoDia.de(Instant.parse("2026-10-21T15:00:00Z"), ECUADOR);

        assertEquals(LocalDate.of(2026, 10, 21), rango.fecha());
        assertEquals(Instant.parse("2026-10-21T05:00:00Z"), rango.inicio());
        assertEquals(Instant.parse("2026-10-22T05:00:00Z"), rango.fin());
    }

    @Test
    void instanteDeNocheEnEcuadorPerteneceAlDiaLocal() {
        RangoDia rango = RangoDia.de(Instant.parse("2026-10-22T03:00:00Z"), ECUADOR);

        assertEquals(LocalDate.of(2026, 10, 21), rango.fecha());
    }

    @Test
    void elFinEsElInicioDelDiaSiguiente() {
        RangoDia hoy = RangoDia.de(Instant.parse("2026-10-21T15:00:00Z"), ECUADOR);
        RangoDia manana = RangoDia.de(hoy.fin(), ECUADOR);

        assertEquals(hoy.fin(), manana.inicio());
        assertEquals(LocalDate.of(2026, 10, 22), manana.fecha());
    }
}

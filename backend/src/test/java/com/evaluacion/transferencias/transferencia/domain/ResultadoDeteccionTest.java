package com.evaluacion.transferencias.transferencia.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResultadoDeteccionTest {

    @Test
    void sinMotivosNoEsSospechosa() {
        ResultadoDeteccion resultado = new ResultadoDeteccion(List.of());

        assertFalse(resultado.esSospechosa());
        assertNull(resultado.motivo());
    }

    @Test
    void conMotivosEsSospechosaYLosUneConPuntoYComa() {
        ResultadoDeteccion resultado = new ResultadoDeteccion(List.of("Monto elevado", "Frecuencia alta"));

        assertTrue(resultado.esSospechosa());
        assertEquals("Monto elevado; Frecuencia alta", resultado.motivo());
    }
}

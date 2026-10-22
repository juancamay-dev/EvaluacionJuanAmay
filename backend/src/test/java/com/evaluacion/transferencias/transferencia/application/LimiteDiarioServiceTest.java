package com.evaluacion.transferencias.transferencia.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.evaluacion.transferencias.transferencia.domain.EvaluacionLimite;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

@ExtendWith(MockitoExtension.class)
class LimiteDiarioServiceTest {

    private static final Long CUENTA = 7L;
    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    TransferenciaRepository transferenciaRepository;

    private LimiteDiarioService service;

    @BeforeEach
    void setUp() {
        service = new LimiteDiarioService(transferenciaRepository, new BigDecimal("10000.00"),
                ZoneId.of("America/Guayaquil"));
    }

    private void acumulado(String monto) {
        when(transferenciaRepository.sumarRealizadasEntre(eq(CUENTA), any(), any())).thenReturn(new BigDecimal(monto));
    }

    @Test
    void consultaElAcumuladoDelDiaCalendarioDeEcuador() {
        acumulado("0");

        service.evaluar(CUENTA, BigDecimal.TEN, Instant.parse("2026-10-22T03:00:00Z"));

        verify(transferenciaRepository).sumarRealizadasEntre(CUENTA, Instant.parse("2026-10-21T05:00:00Z"),
                Instant.parse("2026-10-22T05:00:00Z"));
    }

    @Test
    void permiteUnMontoIgualAlDisponible() {
        acumulado("9000.00");

        EvaluacionLimite evaluacion = service.evaluar(CUENTA, new BigDecimal("1000.00"), AHORA);

        assertFalse(evaluacion.excedido());
    }

    @Test
    void rechazaUnMontoMayorAlDisponibleEInformaElDisponible() {
        acumulado("9000.00");

        EvaluacionLimite evaluacion = service.evaluar(CUENTA, new BigDecimal("1000.01"), AHORA);

        assertTrue(evaluacion.excedido());
        assertEquals(new BigDecimal("1000.00"), evaluacion.disponible());
        assertEquals(new BigDecimal("9000.00"), evaluacion.acumulado());
    }

    @Test
    void disponibleNuncaEsNegativo() {
        acumulado("12000.00");

        EvaluacionLimite evaluacion = service.evaluar(CUENTA, new BigDecimal("1.00"), AHORA);

        assertEquals(0, BigDecimal.ZERO.compareTo(evaluacion.disponible()));
        assertTrue(evaluacion.excedido());
    }

    @Test
    void sinTransferenciasPreviasElDisponibleEsElLimite() {
        acumulado("0");

        EvaluacionLimite evaluacion = service.evaluar(CUENTA, new BigDecimal("10000.00"), AHORA);

        assertEquals(new BigDecimal("10000.00"), evaluacion.disponible());
        assertFalse(evaluacion.excedido());
    }
}

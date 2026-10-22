package com.evaluacion.transferencias.transferencia.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.transferencia.domain.ResultadoDeteccion;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeteccionSospechosaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    TransferenciaRepository transferenciaRepository;

    private DeteccionSospechosaService service;
    private Cuenta origen;

    @BeforeEach
    void setUp() {
        service = new DeteccionSospechosaService(transferenciaRepository, new BigDecimal("5000.00"), 5);
        origen = mock(Cuenta.class);
        when(origen.getId()).thenReturn(7L);
        when(origen.getNumeroCuenta()).thenReturn("1000000001");
        previasEnLaHora(0);
    }

    private void previasEnLaHora(long cantidad) {
        when(transferenciaRepository.contarRealizadasEntre(eq(7L), any(), any())).thenReturn(cantidad);
    }

    @Test
    void montoIgualAlUmbralNoEsSospechoso() {
        assertFalse(service.evaluar(origen, new BigDecimal("5000.00"), AHORA).esSospechosa());
    }

    @Test
    void montoSobreElUmbralEsSospechoso() {
        ResultadoDeteccion resultado = service.evaluar(origen, new BigDecimal("5000.01"), AHORA);

        assertTrue(resultado.esSospechosa());
        assertEquals("Monto superior a $5,000.00", resultado.motivo());
    }

    @Test
    void quintaTransferenciaDeLaHoraNoEsSospechosa() {
        previasEnLaHora(4);

        assertFalse(service.evaluar(origen, new BigDecimal("100.00"), AHORA).esSospechosa());
    }

    @Test
    void sextaTransferenciaDeLaHoraEsSospechosa() {
        previasEnLaHora(5);

        ResultadoDeteccion resultado = service.evaluar(origen, new BigDecimal("100.00"), AHORA);

        assertTrue(resultado.esSospechosa());
        assertEquals("Más de 5 transferencias en la última hora", resultado.motivo());
    }

    @Test
    void cuentaLaUltimaHoraExacta() {
        service.evaluar(origen, new BigDecimal("100.00"), AHORA);

        verify(transferenciaRepository).contarRealizadasEntre(7L, Instant.parse("2026-10-21T14:00:00Z"), AHORA);
    }

    @Test
    void ambasCondicionesRegistranAmbosMotivos() {
        previasEnLaHora(5);

        ResultadoDeteccion resultado = service.evaluar(origen, new BigDecimal("8000.00"), AHORA);

        assertEquals(2, resultado.motivos().size());
        assertEquals("Monto superior a $5,000.00; Más de 5 transferencias en la última hora", resultado.motivo());
    }
}

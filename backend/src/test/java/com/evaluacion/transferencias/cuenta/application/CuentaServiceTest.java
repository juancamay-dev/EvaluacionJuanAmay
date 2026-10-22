package com.evaluacion.transferencias.cuenta.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.cuenta.application.dto.ResumenCuentaResponse;
import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;
import com.evaluacion.transferencias.cuenta.infrastructure.CuentaRepository;
import com.evaluacion.transferencias.shared.error.CuentaDuplicadaException;
import com.evaluacion.transferencias.shared.error.RecursoNoEncontradoException;
import com.evaluacion.transferencias.transferencia.application.LimiteDiarioService;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    @Mock
    CuentaRepository cuentaRepository;
    @Mock
    TransferenciaRepository transferenciaRepository;
    @Mock
    LimiteDiarioService limiteDiarioService;

    private CuentaService service;

    private final CuentaRequest request = new CuentaRequest("1000000001", "Ana Pérez", TipoCuenta.AHORRO,
            new BigDecimal("500.00"), EstadoCuenta.ACTIVA);

    @BeforeEach
    void setUp() {
        service = new CuentaService(cuentaRepository, transferenciaRepository, limiteDiarioService, Clock.fixed(AHORA, ZoneId.of("America/Guayaquil")));
    }

    @Test
    void crearPersisteLaCuentaYDevuelveLaRespuesta() {
        CuentaResponse response = service.crear(request);

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).persistAndFlush(captor.capture());
        Cuenta guardada = captor.getValue();
        assertEquals("1000000001", guardada.getNumeroCuenta());
        assertEquals(AHORA, guardada.getFechaCreacion());
        assertEquals(new BigDecimal("500.00"), guardada.getSaldo());
        assertEquals("1000000001", response.numeroCuenta());
        assertEquals(TipoCuenta.AHORRO, response.tipo());
        assertEquals(EstadoCuenta.ACTIVA, response.estado());
    }

    @Test
    void crearConNumeroExistenteLanzaCuentaDuplicadaSinPersistir() {
        when(cuentaRepository.existePorNumero("1000000001")).thenReturn(true);

        assertThrows(CuentaDuplicadaException.class, () -> service.crear(request));
        verify(cuentaRepository, never()).persistAndFlush(any());
    }

    @Test
    void crearTraduceViolacionUniqueEnCuentaDuplicada() {
        doThrow(new ConstraintViolationException("duplicado", new SQLException("Duplicate entry"),
                "uk_cuenta_numero")).when(cuentaRepository).persistAndFlush(any());

        assertThrows(CuentaDuplicadaException.class, () -> service.crear(request));
    }

    @Test
    void obtenerCuentaInexistenteLanzaRecursoNoEncontrado() {
        when(cuentaRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
    }

    @Test
    void resumenCombinaSaldoTotalesYDisponibleDelDia() {
        Cuenta cuenta = new Cuenta("1000000001", "Ana Pérez", TipoCuenta.AHORRO, new BigDecimal("800.00"),
                EstadoCuenta.ACTIVA, AHORA);
        when(cuentaRepository.findByIdOptional(7L)).thenReturn(Optional.of(cuenta));
        when(limiteDiarioService.acumuladoDelDia(null, AHORA)).thenReturn(new BigDecimal("6200.00"));
        when(limiteDiarioService.getLimiteDiario()).thenReturn(new BigDecimal("10000.00"));
        Instant inicio = Instant.parse("2026-10-21T05:00:00Z");
        Instant fin = Instant.parse("2026-10-22T05:00:00Z");
        when(transferenciaRepository.contarRealizadasEntre(null, inicio, fin)).thenReturn(3L);
        when(transferenciaRepository.contarPorEstadoEntre(null, EstadoTransferencia.EN_REVISION, inicio, fin))
                .thenReturn(1L);
        when(transferenciaRepository.contarPorEstado(null, EstadoTransferencia.EN_REVISION)).thenReturn(4L);

        ResumenCuentaResponse resumen = service.resumen(7L);

        assertEquals("1000000001", resumen.numeroCuenta());
        assertEquals(LocalDate.of(2026, 10, 21), resumen.fecha());
        assertEquals(new BigDecimal("800.00"), resumen.saldoActual());
        assertEquals(new BigDecimal("6200.00"), resumen.totalTransferidoHoy());
        assertEquals(3L, resumen.cantidadTransferenciasHoy());
        assertEquals(1L, resumen.sospechosasHoy());
        assertEquals(4L, resumen.sospechosasTotales());
        assertEquals(new BigDecimal("10000.00"), resumen.limiteDiario());
        assertEquals(new BigDecimal("3800.00"), resumen.disponibleHoy());
    }

    @Test
    void resumenDeCuentaInexistenteLanzaRecursoNoEncontrado() {
        when(cuentaRepository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.resumen(99L));
    }
}

package com.evaluacion.transferencias.transferencia.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;
import com.evaluacion.transferencias.cuenta.infrastructure.CuentaRepository;
import com.evaluacion.transferencias.shared.error.RecursoNoEncontradoException;
import com.evaluacion.transferencias.shared.error.SolicitudInvalidaException;
import com.evaluacion.transferencias.shared.error.TransferenciaRechazadaException;
import com.evaluacion.transferencias.transferencia.application.dto.HistorialFiltro;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaRequest;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaResponse;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.domain.EvaluacionLimite;
import com.evaluacion.transferencias.transferencia.domain.ResultadoDeteccion;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;
import com.evaluacion.transferencias.transferencia.infrastructure.FiltroTransferencias;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferenciaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");
    private static final String ORIGEN = "1000000001";
    private static final String DESTINO = "1000000002";

    @Mock
    CuentaRepository cuentaRepository;
    @Mock
    TransferenciaRepository transferenciaRepository;
    @Mock
    LimiteDiarioService limiteDiarioService;
    @Mock
    DeteccionSospechosaService deteccionSospechosaService;

    private TransferenciaService service;
    private Cuenta origen;
    private Cuenta destino;

    @BeforeEach
    void setUp() {
        service = new TransferenciaService(cuentaRepository, transferenciaRepository, limiteDiarioService,
                deteccionSospechosaService, Clock.fixed(AHORA, ZoneId.of("America/Guayaquil")));
        origen = cuenta(ORIGEN, "1000.00", EstadoCuenta.ACTIVA);
        destino = cuenta(DESTINO, "500.00", EstadoCuenta.ACTIVA);
        registrar(origen);
        registrar(destino);
        limiteDisponible("10000.00", false);
        when(deteccionSospechosaService.evaluar(any(), any(), any())).thenReturn(new ResultadoDeteccion(List.of()));
        doAnswer(inv -> asignarId(inv.getArgument(0), 15L)).when(transferenciaRepository).persist(any(Transferencia.class));
    }

    private static Cuenta cuenta(String numero, String saldo, EstadoCuenta estado) {
        return new Cuenta(numero, "Titular " + numero, TipoCuenta.AHORRO, new BigDecimal(saldo), estado, AHORA);
    }

    private void registrar(Cuenta cuenta) {
        when(cuentaRepository.buscarPorNumeroParaActualizar(cuenta.getNumeroCuenta())).thenReturn(Optional.of(cuenta));
    }

    private void limiteDisponible(String disponible, boolean excedido) {
        when(limiteDiarioService.evaluar(any(), any(), any())).thenReturn(new EvaluacionLimite(
                new BigDecimal("10000.00"), new BigDecimal("10000.00").subtract(new BigDecimal(disponible)),
                new BigDecimal(disponible), excedido));
    }

    private static Object asignarId(Transferencia transferencia, Long id) throws ReflectiveOperationException {
        Field campo = Transferencia.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(transferencia, id);
        return null;
    }

    private static TransferenciaRequest request(String origen, String destino, String monto) {
        return new TransferenciaRequest(origen, destino, new BigDecimal(monto));
    }

    private Transferencia transferenciaGuardada() {
        ArgumentCaptor<Transferencia> captor = ArgumentCaptor.forClass(Transferencia.class);
        verify(transferenciaRepository).persist(captor.capture());
        return captor.getValue();
    }

    @Test
    void transferenciaValidaDebitaAcreditaYGuardaCompletada() {
        TransferenciaResponse response = service.ejecutar(request(ORIGEN, DESTINO, "200.00"));

        assertEquals(new BigDecimal("800.00"), origen.getSaldo());
        assertEquals(new BigDecimal("700.00"), destino.getSaldo());
        assertEquals(EstadoTransferencia.COMPLETADA, transferenciaGuardada().getEstado());
        assertEquals(EstadoTransferencia.COMPLETADA, response.estado());
        assertEquals(15L, response.id());
    }

    @Test
    void transferenciaSospechosaSeEjecutaYQuedaEnRevision() {
        when(deteccionSospechosaService.evaluar(any(), any(), any()))
                .thenReturn(new ResultadoDeteccion(List.of("Monto superior a $5,000.00")));

        TransferenciaResponse response = service.ejecutar(request(ORIGEN, DESTINO, "300.00"));

        assertEquals(new BigDecimal("700.00"), origen.getSaldo());
        assertEquals(EstadoTransferencia.EN_REVISION, response.estado());
        assertEquals("Monto superior a $5,000.00", transferenciaGuardada().getMotivo());
    }

    @Test
    void mismaCuentaLanzaSolicitudInvalidaSinTocarRepositorios() {
        assertThrows(SolicitudInvalidaException.class, () -> service.ejecutar(request(ORIGEN, ORIGEN, "10.00")));
        verifyNoInteractions(cuentaRepository, transferenciaRepository);
    }

    @Test
    void cuentaInexistenteLanza404YNoGuardaNada() {
        when(cuentaRepository.buscarPorNumeroParaActualizar(DESTINO)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.ejecutar(request(ORIGEN, DESTINO, "10.00")));
        verify(transferenciaRepository, never()).persist(any(Transferencia.class));
    }

    @Test
    void bloqueaLasCuentasEnOrdenAscendenteDeNumero() {
        service.ejecutar(request(DESTINO, ORIGEN, "10.00"));

        InOrder orden = inOrder(cuentaRepository);
        orden.verify(cuentaRepository).buscarPorNumeroParaActualizar(ORIGEN);
        orden.verify(cuentaRepository).buscarPorNumeroParaActualizar(DESTINO);
    }

    @Test
    void origenInactivaSeRechazaConCuentaInactiva() {
        registrar(cuenta(ORIGEN, "1000.00", EstadoCuenta.INACTIVA));

        TransferenciaRechazadaException e = assertThrows(TransferenciaRechazadaException.class,
                () -> service.ejecutar(request(ORIGEN, DESTINO, "10.00")));

        assertEquals(TransferenciaRechazadaException.CUENTA_INACTIVA, e.getCodigo());
        assertEquals(422, e.getStatus());
        assertEquals(EstadoTransferencia.RECHAZADA, transferenciaGuardada().getEstado());
    }

    @Test
    void destinoInactivaSeRechazaConCuentaInactiva() {
        registrar(cuenta(DESTINO, "1000.00", EstadoCuenta.INACTIVA));

        TransferenciaRechazadaException e = assertThrows(TransferenciaRechazadaException.class,
                () -> service.ejecutar(request(ORIGEN, DESTINO, "10.00")));

        assertEquals(TransferenciaRechazadaException.CUENTA_INACTIVA, e.getCodigo());
        assertEquals(EstadoTransferencia.RECHAZADA, transferenciaGuardada().getEstado());
    }

    @Test
    void saldoInsuficienteGuardaRechazadaYNoMueveSaldos() {
        TransferenciaRechazadaException e = assertThrows(TransferenciaRechazadaException.class,
                () -> service.ejecutar(request(ORIGEN, DESTINO, "1000.01")));

        assertEquals(TransferenciaRechazadaException.SALDO_INSUFICIENTE, e.getCodigo());
        assertEquals(EstadoTransferencia.RECHAZADA, transferenciaGuardada().getEstado());
        assertEquals(new BigDecimal("1000.00"), origen.getSaldo());
        assertEquals(new BigDecimal("500.00"), destino.getSaldo());
        verifyNoInteractions(deteccionSospechosaService);
    }

    @Test
    void limiteExcedidoInformaElDisponibleEnElMensaje() {
        limiteDisponible("150.00", true);

        TransferenciaRechazadaException e = assertThrows(TransferenciaRechazadaException.class,
                () -> service.ejecutar(request(ORIGEN, DESTINO, "200.00")));

        assertEquals(TransferenciaRechazadaException.LIMITE_DIARIO_EXCEDIDO, e.getCodigo());
        assertTrue(e.getMessage().contains("$150.00"), e.getMessage());
        assertEquals(new BigDecimal("1000.00"), origen.getSaldo());
    }

    @Test
    void excepcionDeRechazoLlevaElIdDeLaTransferenciaGuardada() {
        TransferenciaRechazadaException e = assertThrows(TransferenciaRechazadaException.class,
                () -> service.ejecutar(request(ORIGEN, DESTINO, "5000.00")));

        assertEquals(15L, e.getTransferenciaId());
    }

    @SuppressWarnings("unchecked")
    private FiltroTransferencias filtroConsultado(HistorialFiltro filtro) {
        PanacheQuery<Transferencia> consulta = mock(PanacheQuery.class);
        when(consulta.page(0, 20)).thenReturn(consulta);
        when(consulta.list()).thenReturn(List.of());
        when(transferenciaRepository.buscar(any())).thenReturn(consulta);

        service.historial(filtro, 0, 20);

        ArgumentCaptor<FiltroTransferencias> captor = ArgumentCaptor.forClass(FiltroTransferencias.class);
        verify(transferenciaRepository).buscar(captor.capture());
        return captor.getValue();
    }

    @Test
    void historialConvierteFechasLocalesAIntervaloUtc() {
        FiltroTransferencias filtro = filtroConsultado(
                new HistorialFiltro(ORIGEN, DESTINO, "2026-10-01", "2026-10-31", "en_revision"));

        assertEquals(Instant.parse("2026-10-01T05:00:00Z"), filtro.desde());
        assertEquals(Instant.parse("2026-11-01T05:00:00Z"), filtro.hasta());
        assertEquals(ORIGEN, filtro.cuentaOrigen());
        assertEquals(DESTINO, filtro.cuentaDestino());
        assertEquals(EstadoTransferencia.EN_REVISION, filtro.estado());
    }

    @Test
    void historialConDesdePosteriorAHastaLanzaSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.historial(
                new HistorialFiltro(null, null, "2026-10-31", "2026-10-01", null), 0, 20));
    }

    @Test
    void historialConEstadoInexistenteLanzaSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.historial(
                new HistorialFiltro(null, null, null, null, "PENDIENTE"), 0, 20));
        assertEquals(EstadoTransferencia.RECHAZADA,
                filtroConsultado(new HistorialFiltro(null, null, null, null, "Rechazada")).estado());
    }

    @Test
    void historialConFechaMalFormadaLanzaSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.historial(
                new HistorialFiltro(null, null, "21/10/2026", null, null), 0, 20));
    }
}

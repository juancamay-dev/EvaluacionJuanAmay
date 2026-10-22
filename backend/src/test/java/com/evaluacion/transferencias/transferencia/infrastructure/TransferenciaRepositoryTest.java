package com.evaluacion.transferencias.transferencia.infrastructure;

import static com.evaluacion.transferencias.support.DatosPrueba.AHORA;
import static com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia.COMPLETADA;
import static com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia.EN_REVISION;
import static com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia.RECHAZADA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.support.DatosPrueba;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestTransaction
class TransferenciaRepositoryTest {

    private static final Instant DESDE = AHORA.minus(Duration.ofHours(1));
    private static final Instant HASTA = AHORA.plus(Duration.ofHours(1));

    @Inject
    TransferenciaRepository repository;

    @Inject
    DatosPrueba datos;

    private Cuenta origen;
    private Cuenta destino;

    /** Dentro de cada prueba: @TestTransaction revierte lo creado en @BeforeEach. */
    private void crearCuentas() {
        origen = datos.cuenta(DatosPrueba.numeroUnico(), "100000.00");
        destino = datos.cuenta(DatosPrueba.numeroUnico(), "100000.00");
    }

    @Test
    void sumarRealizadasEntreSumaCompletadasYEnRevision() {
        crearCuentas();
        datos.transferencia(origen, destino, "100.00", COMPLETADA, AHORA);
        datos.transferencia(origen, destino, "200.00", EN_REVISION, AHORA);

        BigDecimal total = repository.sumarRealizadasEntre(origen.getId(), DESDE, HASTA);

        assertEquals(0, new BigDecimal("300.00").compareTo(total));
    }

    @Test
    void sumarRealizadasEntreExcluyeRechazadas() {
        crearCuentas();
        datos.transferencia(origen, destino, "100.00", COMPLETADA, AHORA);
        datos.transferencia(origen, destino, "900.00", RECHAZADA, AHORA);

        BigDecimal total = repository.sumarRealizadasEntre(origen.getId(), DESDE, HASTA);

        assertEquals(0, new BigDecimal("100.00").compareTo(total));
    }

    @Test
    void sumarRealizadasEntreRespetaElIntervaloSemiabierto() {
        crearCuentas();
        datos.transferencia(origen, destino, "10.00", COMPLETADA, DESDE);
        datos.transferencia(origen, destino, "20.00", COMPLETADA, HASTA);

        BigDecimal total = repository.sumarRealizadasEntre(origen.getId(), DESDE, HASTA);

        assertEquals(0, new BigDecimal("10.00").compareTo(total));
    }

    @Test
    void sumarRealizadasEntreSinDatosDevuelveCero() {
        crearCuentas();
        BigDecimal total = repository.sumarRealizadasEntre(origen.getId(), DESDE, HASTA);

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void contarRealizadasEntreSoloCuentaLaCuentaOrigen() {
        crearCuentas();
        datos.transferencia(origen, destino, "10.00", COMPLETADA, AHORA);
        datos.transferencia(origen, destino, "10.00", EN_REVISION, AHORA);
        datos.transferencia(origen, destino, "10.00", RECHAZADA, AHORA);
        datos.transferencia(destino, origen, "10.00", COMPLETADA, AHORA);

        assertEquals(2, repository.contarRealizadasEntre(origen.getId(), DESDE, HASTA));
    }

    @Test
    void contarPorEstadoEntreYContarPorEstado() {
        crearCuentas();
        datos.transferencia(origen, destino, "10.00", EN_REVISION, AHORA);
        datos.transferencia(origen, destino, "10.00", EN_REVISION, AHORA.minus(Duration.ofDays(3)));
        datos.transferencia(origen, destino, "10.00", COMPLETADA, AHORA);

        assertEquals(1, repository.contarPorEstadoEntre(origen.getId(), EN_REVISION, DESDE, HASTA));
        assertEquals(2, repository.contarPorEstado(origen.getId(), EN_REVISION));
    }

    @Test
    void buscarFiltraPorCadaCriterioYSusCombinaciones() {
        crearCuentas();
        Cuenta otra = datos.cuenta(DatosPrueba.numeroUnico(), "1000.00");
        Transferencia a = datos.transferencia(origen, destino, "10.00", COMPLETADA, AHORA);
        Transferencia b = datos.transferencia(origen, otra, "20.00", RECHAZADA, AHORA.minus(Duration.ofDays(2)));
        Transferencia c = datos.transferencia(otra, destino, "30.00", EN_REVISION, AHORA);

        assertEquals(List.of(a.getId(), b.getId()),
                ids(new FiltroTransferencias(origen.getNumeroCuenta(), null, null, null, null)));
        assertEquals(List.of(c.getId(), a.getId()),
                ids(new FiltroTransferencias(null, destino.getNumeroCuenta(), null, null, null)));
        assertEquals(List.of(b.getId()),
                ids(new FiltroTransferencias(origen.getNumeroCuenta(), null, null, null, RECHAZADA)));
        assertEquals(List.of(a.getId()),
                ids(new FiltroTransferencias(origen.getNumeroCuenta(), null, DESDE, HASTA, null)));
        assertEquals(List.of(b.getId()),
                ids(new FiltroTransferencias(origen.getNumeroCuenta(), otra.getNumeroCuenta(), null, DESDE, null)));
    }

    @Test
    void buscarOrdenaPorFechaDescendenteYLuegoIdDescendente() {
        crearCuentas();
        Transferencia antigua = datos.transferencia(origen, destino, "10.00", COMPLETADA,
                AHORA.minus(Duration.ofMinutes(30)));
        Transferencia primera = datos.transferencia(origen, destino, "10.00", COMPLETADA, AHORA);
        Transferencia segunda = datos.transferencia(origen, destino, "10.00", COMPLETADA, AHORA);

        assertEquals(List.of(segunda.getId(), primera.getId(), antigua.getId()),
                ids(new FiltroTransferencias(origen.getNumeroCuenta(), null, null, null, null)));
    }

    private List<Long> ids(FiltroTransferencias filtro) {
        return repository.buscar(filtro).list().stream().map(Transferencia::getId).toList();
    }
}

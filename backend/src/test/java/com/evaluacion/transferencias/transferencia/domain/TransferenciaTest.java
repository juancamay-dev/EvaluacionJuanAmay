package com.evaluacion.transferencias.transferencia.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;

class TransferenciaTest {

    private static final Instant FECHA = Instant.parse("2026-10-21T15:00:00Z");
    private static final BigDecimal MONTO = new BigDecimal("250.00");

    private final Cuenta origen = cuenta("1000000001");
    private final Cuenta destino = cuenta("1000000002");

    private static Cuenta cuenta(String numero) {
        return new Cuenta(numero, "Titular " + numero, TipoCuenta.AHORRO, new BigDecimal("1000.00"),
                EstadoCuenta.ACTIVA, FECHA);
    }

    @Test
    void completadaNaceSinMotivo() {
        Transferencia transferencia = Transferencia.completada(origen, destino, MONTO, FECHA);

        assertSame(origen, transferencia.getCuentaOrigen());
        assertSame(destino, transferencia.getCuentaDestino());
        assertEquals(MONTO, transferencia.getMonto());
        assertEquals(EstadoTransferencia.COMPLETADA, transferencia.getEstado());
        assertNull(transferencia.getMotivo());
        assertEquals(FECHA, transferencia.getFecha());
    }

    @Test
    void enRevisionGuardaElMotivo() {
        Transferencia transferencia = Transferencia.enRevision(origen, destino, MONTO, "Monto elevado", FECHA);

        assertEquals(EstadoTransferencia.EN_REVISION, transferencia.getEstado());
        assertEquals("Monto elevado", transferencia.getMotivo());
    }

    @Test
    void rechazadaGuardaElMotivo() {
        Transferencia transferencia = Transferencia.rechazada(origen, destino, MONTO, "Saldo insuficiente", FECHA);

        assertEquals(EstadoTransferencia.RECHAZADA, transferencia.getEstado());
        assertEquals("Saldo insuficiente", transferencia.getMotivo());
    }

    @Test
    void montoNoPositivoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.completada(origen, destino, BigDecimal.ZERO, FECHA));
        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.completada(origen, destino, new BigDecimal("-5.00"), FECHA));
    }

    @Test
    void origenYDestinoIgualesLanzaExcepcion() {
        Cuenta mismaCuenta = cuenta("1000000001");

        assertThrows(IllegalArgumentException.class,
                () -> Transferencia.completada(origen, mismaCuenta, MONTO, FECHA));
    }
}

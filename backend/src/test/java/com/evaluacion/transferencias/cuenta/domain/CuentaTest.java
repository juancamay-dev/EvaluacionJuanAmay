package com.evaluacion.transferencias.cuenta.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CuentaTest {

    private static final Instant FECHA = Instant.parse("2026-10-21T15:00:00Z");

    private static Cuenta cuentaConSaldo(String saldo) {
        return new Cuenta("1000000001", "Ana Pérez", TipoCuenta.AHORRO, new BigDecimal(saldo),
                EstadoCuenta.ACTIVA, FECHA);
    }

    @Test
    void crearCuentaValidaConservaSusDatos() {
        Cuenta cuenta = new Cuenta("1000000001", "Ana Pérez", TipoCuenta.CORRIENTE, new BigDecimal("150.00"),
                EstadoCuenta.ACTIVA, FECHA);

        assertEquals("1000000001", cuenta.getNumeroCuenta());
        assertEquals("Ana Pérez", cuenta.getTitular());
        assertEquals(TipoCuenta.CORRIENTE, cuenta.getTipo());
        assertEquals(new BigDecimal("150.00"), cuenta.getSaldo());
        assertEquals(EstadoCuenta.ACTIVA, cuenta.getEstado());
        assertEquals(FECHA, cuenta.getFechaCreacion());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"123", "12345678901", "12345abcde"})
    void numeroDeCuentaInvalidoLanzaExcepcion(String numero) {
        assertThrows(IllegalArgumentException.class, () -> new Cuenta(numero, "Ana Pérez", TipoCuenta.AHORRO,
                BigDecimal.TEN, EstadoCuenta.ACTIVA, FECHA));
    }

    @Test
    void saldoInicialNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> cuentaConSaldo("-0.01"));
    }

    @Test
    void debitarRestaDelSaldo() {
        Cuenta cuenta = cuentaConSaldo("100.00");

        cuenta.debitar(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), cuenta.getSaldo());
    }

    @Test
    void debitarConSaldoInsuficienteLanzaExcepcionYNoModificaSaldo() {
        Cuenta cuenta = cuentaConSaldo("100.00");

        assertThrows(IllegalStateException.class, () -> cuenta.debitar(new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), cuenta.getSaldo());
    }

    @Test
    void debitarTodoElSaldoEsPermitido() {
        Cuenta cuenta = cuentaConSaldo("100.00");

        cuenta.debitar(new BigDecimal("100.00"));

        assertEquals(0, cuenta.getSaldo().compareTo(BigDecimal.ZERO));
    }

    @Test
    void acreditarSumaAlSaldo() {
        Cuenta cuenta = cuentaConSaldo("70.00");

        cuenta.acreditar(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("100.00"), cuenta.getSaldo());
    }

    @Test
    void montoNoPositivoEnDebitarOAcreditarLanzaExcepcion() {
        Cuenta cuenta = cuentaConSaldo("100.00");

        assertThrows(IllegalArgumentException.class, () -> cuenta.debitar(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> cuenta.debitar(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> cuenta.acreditar(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> cuenta.acreditar(new BigDecimal("-1.00")));
        assertEquals(new BigDecimal("100.00"), cuenta.getSaldo());
    }

    @Test
    void tieneSaldoSuficienteEsInclusivo() {
        Cuenta cuenta = cuentaConSaldo("100.00");

        assertTrue(cuenta.tieneSaldoSuficiente(new BigDecimal("100.00")));
        assertFalse(cuenta.tieneSaldoSuficiente(new BigDecimal("100.01")));
    }

    @Test
    void estaActivaSegunEstado() {
        Cuenta activa = cuentaConSaldo("10.00");
        Cuenta inactiva = new Cuenta("1000000002", "Luis Mora", TipoCuenta.AHORRO, BigDecimal.TEN,
                EstadoCuenta.INACTIVA, FECHA);

        assertTrue(activa.estaActiva());
        assertFalse(inactiva.estaActiva());
    }
}

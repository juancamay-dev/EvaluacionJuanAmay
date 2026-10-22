package com.evaluacion.transferencias.cuenta.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;
import com.evaluacion.transferencias.support.DatosPrueba;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestTransaction
class CuentaRepositoryTest {

    @Inject
    CuentaRepository repository;

    @Inject
    DatosPrueba datos;

    @Test
    void existePorNumeroDetectaCuentaRegistrada() {
        String numero = DatosPrueba.numeroUnico();
        datos.cuenta(numero, "100.00");

        assertTrue(repository.existePorNumero(numero));
        assertFalse(repository.existePorNumero(DatosPrueba.numeroUnico()));
    }

    @Test
    void buscarPorNumeroParaActualizarDevuelveLaCuenta() {
        String numero = DatosPrueba.numeroUnico();
        datos.cuenta(numero, "250.00");

        Optional<Cuenta> cuenta = repository.buscarPorNumeroParaActualizar(numero);

        assertTrue(cuenta.isPresent());
        assertEquals(numero, cuenta.get().getNumeroCuenta());
        assertEquals(0, new BigDecimal("250.00").compareTo(cuenta.get().getSaldo()));
    }

    @Test
    void buscarPorNumeroParaActualizarDevuelveVacioSiNoExiste() {
        assertTrue(repository.buscarPorNumeroParaActualizar(DatosPrueba.numeroUnico()).isEmpty());
    }

    @Test
    void restriccionUniqueRechazaNumeroDuplicado() {
        String numero = DatosPrueba.numeroUnico();
        datos.cuenta(numero, "100.00");
        Cuenta duplicada = new Cuenta(numero, "Otro titular", TipoCuenta.CORRIENTE, BigDecimal.TEN,
                EstadoCuenta.ACTIVA, DatosPrueba.AHORA);

        assertThrows(ConstraintViolationException.class, () -> repository.persistAndFlush(duplicada));
    }

    @Test
    void listarOrdenadasDevuelvePorNumeroAscendente() {
        String mayor = DatosPrueba.numeroUnico();
        String menor = "1" + mayor.substring(1);
        datos.cuenta(mayor, "10.00");
        datos.cuenta(menor, "10.00");

        List<String> numeros = repository.listarOrdenadas().list().stream()
                .map(Cuenta::getNumeroCuenta)
                .filter(n -> n.equals(mayor) || n.equals(menor))
                .toList();

        assertEquals(List.of(menor, mayor), numeros);
    }
}

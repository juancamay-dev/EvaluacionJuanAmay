package com.evaluacion.transferencias.transferencia.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.support.DatosPrueba;

import io.quarkus.test.junit.QuarkusTest;

/** Demuestra que el bloqueo pesimista en orden evita el doble gasto y los deadlocks. */
@QuarkusTest
class TransferenciaConcurrenciaTest {

    @Inject
    DatosPrueba datos;

    private static float saldo(Cuenta cuenta) {
        return given().get("/api/cuentas/" + cuenta.getId()).then().extract().path("saldo");
    }

    private static List<Integer> enParalelo(List<Callable<Integer>> tareas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tareas.size());
        try {
            List<Integer> status = new ArrayList<>();
            for (Future<Integer> resultado : executor.invokeAll(tareas, 60, TimeUnit.SECONDS)) {
                status.add(resultado.get());
            }
            return status;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<Integer> transferencia(Cuenta origen, Cuenta destino, String monto) {
        return () -> TransferenciaResourceTest.transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), monto)
                .extract().statusCode();
    }

    @Test
    void transferenciasConcurrentesNuncaDejanSaldoNegativo() throws Exception {
        Cuenta origen = datos.cuenta(DatosPrueba.numeroUnico(), "1000.00");
        Cuenta destino = datos.cuenta(DatosPrueba.numeroUnico(), "0.00");
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tareas.add(transferencia(origen, destino, "200.00"));
        }

        List<Integer> status = enParalelo(tareas);

        assertEquals(5, status.stream().filter(s -> s == 201).count());
        assertEquals(5, status.stream().filter(s -> s == 422).count());
        assertEquals(0.00f, saldo(origen));
        assertEquals(1000.00f, saldo(destino));
    }

    @Test
    void transferenciasCruzadasNoProducenDeadlock() throws Exception {
        Cuenta a = datos.cuenta(DatosPrueba.numeroUnico(), "1000.00");
        Cuenta b = datos.cuenta(DatosPrueba.numeroUnico(), "1000.00");
        List<Callable<Integer>> tareas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tareas.add(transferencia(a, b, "10.00"));
            tareas.add(transferencia(b, a, "10.00"));
        }

        List<Integer> status = enParalelo(tareas);

        assertEquals(20, status.stream().filter(s -> s == 201).count());
        assertEquals(2000.00f, saldo(a) + saldo(b));
    }
}

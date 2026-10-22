package com.evaluacion.transferencias.transferencia.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.support.DatosPrueba;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HistorialResourceTest {

    @Inject
    DatosPrueba datos;

    private Cuenta cuenta() {
        return datos.cuenta(DatosPrueba.numeroUnico(), "100000.00");
    }

    private Transferencia transferencia(Cuenta origen, Cuenta destino, EstadoTransferencia estado, String fecha) {
        return datos.transferencia(origen, destino, "10.00", estado, Instant.parse(fecha));
    }

    private static int id(Transferencia transferencia) {
        return transferencia.getId().intValue();
    }

    @Test
    void historialFiltraPorCuentaOrigenYEstado() {
        Cuenta origen = cuenta();
        Cuenta destino = cuenta();
        Transferencia rechazada = transferencia(origen, destino, EstadoTransferencia.RECHAZADA, "2026-10-21T15:00:00Z");
        transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-21T15:00:00Z");
        transferencia(destino, origen, EstadoTransferencia.RECHAZADA, "2026-10-21T15:00:00Z");

        given().queryParam("cuentaOrigen", origen.getNumeroCuenta()).queryParam("estado", "rechazada")
                .when().get("/api/transferencias")
                .then().statusCode(200)
                .body("totalElementos", equalTo(1))
                .body("contenido.id", contains(id(rechazada)))
                .body("contenido[0].estado", equalTo("RECHAZADA"));
    }

    @Test
    void historialFiltraPorCuentaDestino() {
        Cuenta origen = cuenta();
        Cuenta destino = cuenta();
        Transferencia recibida = transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-21T15:00:00Z");
        transferencia(destino, origen, EstadoTransferencia.COMPLETADA, "2026-10-21T15:00:00Z");

        given().queryParam("cuentaDestino", destino.getNumeroCuenta())
                .when().get("/api/transferencias")
                .then().statusCode(200)
                .body("contenido.id", contains(id(recibida)))
                .body("contenido[0].cuentaDestino", equalTo(destino.getNumeroCuenta()));
    }

    @Test
    void historialFiltraPorRangoDeFechasInclusivo() {
        Cuenta origen = cuenta();
        Cuenta destino = cuenta();
        transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-20T04:59:59Z");
        Transferencia inicioDelDia = transferencia(origen, destino, EstadoTransferencia.COMPLETADA,
                "2026-10-20T05:00:00Z");
        Transferencia finDelDia = transferencia(origen, destino, EstadoTransferencia.COMPLETADA,
                "2026-10-21T04:59:59Z");
        transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-21T05:00:00Z");

        given().queryParam("cuentaOrigen", origen.getNumeroCuenta())
                .queryParam("desde", "2026-10-20").queryParam("hasta", "2026-10-20")
                .when().get("/api/transferencias")
                .then().statusCode(200)
                .body("contenido.id", contains(id(finDelDia), id(inicioDelDia)));
    }

    @Test
    void historialOrdenaPorFechaDescendente() {
        Cuenta origen = cuenta();
        Cuenta destino = cuenta();
        Transferencia antigua = transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-01T15:00:00Z");
        Transferencia reciente = transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-21T15:00:00Z");
        Transferencia intermedia = transferencia(origen, destino, EstadoTransferencia.EN_REVISION,
                "2026-10-10T15:00:00Z");

        given().queryParam("cuentaOrigen", origen.getNumeroCuenta())
                .when().get("/api/transferencias")
                .then().statusCode(200)
                .body("contenido.id", contains(id(reciente), id(intermedia), id(antigua)));
    }

    @Test
    void historialPaginaResultados() {
        Cuenta origen = cuenta();
        Cuenta destino = cuenta();
        for (int i = 1; i <= 5; i++) {
            transferencia(origen, destino, EstadoTransferencia.COMPLETADA, "2026-10-0" + i + "T15:00:00Z");
        }

        given().queryParam("cuentaOrigen", origen.getNumeroCuenta()).queryParam("pagina", 1).queryParam("tamano", 2)
                .when().get("/api/transferencias")
                .then().statusCode(200)
                .body("pagina", equalTo(1))
                .body("tamano", equalTo(2))
                .body("totalElementos", equalTo(5))
                .body("totalPaginas", equalTo(3))
                .body("contenido.size()", equalTo(2));
    }

    @Test
    void historialConFiltrosInvalidosDevuelve400() {
        given().queryParam("estado", "PENDIENTE").when().get("/api/transferencias")
                .then().statusCode(400).body("codigo", equalTo("SOLICITUD_INVALIDA"));
        given().queryParam("desde", "2026-10-31").queryParam("hasta", "2026-10-01").when().get("/api/transferencias")
                .then().statusCode(400).body("codigo", equalTo("SOLICITUD_INVALIDA"));
        given().queryParam("cuentaOrigen", "123").when().get("/api/transferencias")
                .then().statusCode(400).body("codigo", equalTo("VALIDACION"));
    }
}

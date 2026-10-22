package com.evaluacion.transferencias.cuenta.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;

import com.evaluacion.transferencias.support.ApiPrueba;
import com.evaluacion.transferencias.support.DatosPrueba;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CuentaResourceTest {

    @Inject
    DatosPrueba datos;

    private static String cuerpo(String numero, String saldo, String tipo, String estado) {
        return """
                {"numeroCuenta":"%s","titular":"Ana Pérez","tipo":"%s","saldoInicial":%s,"estado":"%s"}
                """.formatted(numero, tipo, saldo, estado);
    }

    private static String cuerpoValido(String numero) {
        return cuerpo(numero, "1500.00", "AHORRO", "ACTIVA");
    }

    @Test
    void postCreaCuentaYDevuelve201ConLocation() {
        String numero = DatosPrueba.numeroUnico();

        var response = given().contentType(ContentType.JSON).body(cuerpoValido(numero))
                .when().post("/api/cuentas")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("numeroCuenta", equalTo(numero))
                .body("saldo", equalTo(1500.00f))
                .extract();

        int id = response.path("id");
        assertThat(response.header("Location"), endsWith("/api/cuentas/" + id));
    }

    @Test
    void postDuplicadoDevuelve409() {
        String numero = DatosPrueba.numeroUnico();
        given().contentType(ContentType.JSON).body(cuerpoValido(numero)).post("/api/cuentas").then().statusCode(201);

        given().contentType(ContentType.JSON).body(cuerpoValido(numero))
                .when().post("/api/cuentas")
                .then().statusCode(409)
                .body("codigo", equalTo("CUENTA_DUPLICADA"));
    }

    @Test
    void postSaldoNegativoDevuelve400ConDetalleDelCampo() {
        given().contentType(ContentType.JSON).body(cuerpo(DatosPrueba.numeroUnico(), "-10.00", "AHORRO", "ACTIVA"))
                .when().post("/api/cuentas")
                .then().statusCode(400)
                .body("codigo", equalTo("VALIDACION"))
                .body("detalles", hasItem(startsWith("saldoInicial: ")));
    }

    @Test
    void postNumeroConMenosDeDiezDigitosDevuelve400() {
        given().contentType(ContentType.JSON).body(cuerpo("12345", "10.00", "AHORRO", "ACTIVA"))
                .when().post("/api/cuentas")
                .then().statusCode(400)
                .body("codigo", equalTo("VALIDACION"))
                .body("detalles", hasItem(startsWith("numeroCuenta: ")));
    }

    @Test
    void postAceptaTipoYEstadoEnMinusculas() {
        given().contentType(ContentType.JSON).body(cuerpo(DatosPrueba.numeroUnico(), "10.00", "ahorro", "activa"))
                .when().post("/api/cuentas")
                .then().statusCode(201)
                .body("tipo", equalTo("AHORRO"))
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    void postJsonMalFormadoDevuelve400() {
        given().contentType(ContentType.JSON).body("{\"numeroCuenta\": ")
                .when().post("/api/cuentas")
                .then().statusCode(400)
                .body("codigo", equalTo("JSON_INVALIDO"));
    }

    @Test
    void postTipoDesconocidoDevuelve400ConFormatoDeLaApi() {
        given().contentType(ContentType.JSON).body(cuerpo(DatosPrueba.numeroUnico(), "10.00", "PLAZO", "ACTIVA"))
                .when().post("/api/cuentas")
                .then().statusCode(400)
                .body("codigo", equalTo("JSON_INVALIDO"))
                .body("detalles", hasItem(startsWith("tipo: valor 'PLAZO' no permitido")));
    }

    @Test
    void getListaCuentasPaginadas() {
        String numero = DatosPrueba.numeroUnico();
        given().contentType(ContentType.JSON).body(cuerpoValido(numero)).post("/api/cuentas").then().statusCode(201);

        given().when().get("/api/cuentas?pagina=0&tamano=500")
                .then().statusCode(200)
                .body("contenido.numeroCuenta", hasItem(numero))
                .body("totalElementos", greaterThanOrEqualTo(1))
                .body("pagina", equalTo(0))
                .body("tamano", equalTo(500));
    }

    @Test
    void getPorIdInexistenteDevuelve404() {
        given().when().get("/api/cuentas/999999999")
                .then().statusCode(404)
                .body("codigo", equalTo("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void resumenReflejaLasOperacionesDelDia() {
        Cuenta origen = datos.cuenta(DatosPrueba.numeroUnico(), "6000.00");
        Cuenta destino = datos.cuenta(DatosPrueba.numeroUnico(), "0.00");
        ApiPrueba.transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "100.00")
                .statusCode(201);
        ApiPrueba.transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "5000.01")
                .statusCode(201);
        ApiPrueba.transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "1000.00")
                .statusCode(422);

        given().when().get("/api/cuentas/" + origen.getId() + "/resumen")
                .then().statusCode(200)
                .body("numeroCuenta", equalTo(origen.getNumeroCuenta()))
                .body("saldoActual", equalTo(899.99f))
                .body("totalTransferidoHoy", equalTo(5100.01f))
                .body("cantidadTransferenciasHoy", equalTo(2))
                .body("sospechosasHoy", equalTo(1))
                .body("sospechosasTotales", equalTo(1))
                .body("limiteDiario", equalTo(10000.00f))
                .body("disponibleHoy", equalTo(4899.99f));
    }

    @Test
    void resumenDeCuentaInexistenteDevuelve404() {
        given().when().get("/api/cuentas/999999999/resumen")
                .then().statusCode(404)
                .body("codigo", equalTo("RECURSO_NO_ENCONTRADO"));
    }
}

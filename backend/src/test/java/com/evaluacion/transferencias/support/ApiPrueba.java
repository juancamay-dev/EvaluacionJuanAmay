package com.evaluacion.transferencias.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/** Llamadas HTTP reutilizadas por las pruebas de integración. */
public final class ApiPrueba {

    private ApiPrueba() {
    }

    public static ValidatableResponse transferir(String origen, String destino, String monto) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"cuentaOrigen":"%s","cuentaDestino":"%s","monto":%s}
                        """.formatted(origen, destino, monto))
                .when().post("/api/transferencias")
                .then();
    }
}

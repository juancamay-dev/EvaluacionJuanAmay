package com.evaluacion.transferencias;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;

@QuarkusTest
class OpenApiTest {

    private static final List<String> PATHS = List.of("/api/cuentas", "/api/cuentas/{id}",
            "/api/cuentas/{id}/resumen", "/api/transferencias");

    private static JsonPath contrato() {
        return given().queryParam("format", "json").when().get("/q/openapi")
                .then().statusCode(200).extract().jsonPath();
    }

    @Test
    void documentaTodosLosEndpoints() {
        Map<String, Object> paths = contrato().getMap("paths");

        PATHS.forEach(path -> assertTrue(paths.containsKey(path), "Falta el path " + path));
    }

    @Test
    void cadaOperacionTieneResumen() {
        JsonPath contrato = contrato();
        for (String path : PATHS) {
            Map<String, Map<String, Object>> operaciones = contrato.getMap("paths.'" + path + "'");
            operaciones.forEach((metodo, operacion) ->
                    assertNotNull(operacion.get("summary"), "Sin summary: " + metodo + " " + path));
        }
    }

    @Test
    void postTransferenciasDocumenta201_400_404_422() {
        Map<String, Object> respuestas = contrato().getMap("paths.'/api/transferencias'.post.responses");

        List.of("201", "400", "404", "422").forEach(codigo ->
                assertTrue(respuestas.containsKey(codigo), "Falta el código " + codigo));
    }

    @Test
    void postTransferenciasIncluyeEjemploDeRequest() {
        Map<String, Object> contenido = contrato()
                .getMap("paths.'/api/transferencias'.post.requestBody.content.'application/json'");

        assertFalse(contenido.get("examples") == null && contenido.get("example") == null,
                "El request de POST /api/transferencias no tiene ejemplo");
    }
}

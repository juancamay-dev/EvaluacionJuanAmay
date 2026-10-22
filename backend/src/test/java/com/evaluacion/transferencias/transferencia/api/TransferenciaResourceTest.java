package com.evaluacion.transferencias.transferencia.api;

import static com.evaluacion.transferencias.support.ApiPrueba.transferir;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.support.DatosPrueba;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TransferenciaResourceTest {

    @Inject
    DatosPrueba datos;

    @Inject
    TransferenciaRepository transferenciaRepository;

    private Cuenta cuenta(String saldo) {
        return datos.cuenta(DatosPrueba.numeroUnico(), saldo);
    }

    private static float saldo(Cuenta cuenta) {
        return given().get("/api/cuentas/" + cuenta.getId()).then().statusCode(200).extract().path("saldo");
    }

    @Test
    void transferenciaValidaDevuelve201YActualizaAmbosSaldos() {
        Cuenta origen = cuenta("1000.00");
        Cuenta destino = cuenta("500.00");

        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "250.50")
                .statusCode(201)
                .body("id", notNullValue())
                .body("estado", equalTo("COMPLETADA"))
                .body("monto", equalTo(250.50f));

        assertEquals(749.50f, saldo(origen));
        assertEquals(750.50f, saldo(destino));
    }

    @Test
    void montoSobre5000Devuelve201EnRevision() {
        Cuenta origen = cuenta("9000.00");
        Cuenta destino = cuenta("0.00");

        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "5000.01")
                .statusCode(201)
                .body("estado", equalTo("EN_REVISION"))
                .body("motivo", containsString("5,000.00"));
    }

    @Test
    void sextaTransferenciaDeLaHoraQuedaEnRevision() {
        Cuenta origen = cuenta("1000.00");
        Cuenta destino = cuenta("0.00");
        for (int i = 0; i < 5; i++) {
            transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "10.00")
                    .statusCode(201).body("estado", equalTo("COMPLETADA"));
        }

        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "10.00")
                .statusCode(201)
                .body("estado", equalTo("EN_REVISION"))
                .body("motivo", containsString("última hora"));
    }

    @Test
    void saldoInsuficienteDevuelve422YQuedaRegistradaComoRechazada() {
        Cuenta origen = cuenta("100.00");
        Cuenta destino = cuenta("0.00");

        String detalle = transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "100.01")
                .statusCode(422)
                .body("codigo", equalTo("SALDO_INSUFICIENTE"))
                .extract().path("detalles[0]");

        Long id = Long.valueOf(detalle.replace("transferenciaId: ", ""));
        Transferencia guardada = transferenciaRepository.findById(id);
        assertEquals(EstadoTransferencia.RECHAZADA, guardada.getEstado());
        assertEquals(100.00f, saldo(origen));
    }

    @Test
    void limiteDiarioExcedidoDevuelve422ConMontoDisponible() {
        Cuenta origen = cuenta("20000.00");
        Cuenta destino = cuenta("0.00");
        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "4500.00").statusCode(201);
        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "4500.00").statusCode(201);

        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "1000.01")
                .statusCode(422)
                .body("codigo", equalTo("LIMITE_DIARIO_EXCEDIDO"))
                .body("mensaje", containsString("$1,000.00"));
    }

    @Test
    void cuentaInactivaDevuelve422() {
        Cuenta origen = cuenta("1000.00");
        Cuenta inactiva = datos.cuenta(DatosPrueba.numeroUnico(), "0.00", EstadoCuenta.INACTIVA);

        transferir(origen.getNumeroCuenta(), inactiva.getNumeroCuenta(), "10.00")
                .statusCode(422)
                .body("codigo", equalTo("CUENTA_INACTIVA"));
    }

    @Test
    void cuentaInexistenteDevuelve404() {
        Cuenta origen = cuenta("1000.00");

        transferir(origen.getNumeroCuenta(), DatosPrueba.numeroUnico(), "10.00")
                .statusCode(404)
                .body("codigo", equalTo("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    void mismaCuentaDevuelve400() {
        Cuenta origen = cuenta("1000.00");

        transferir(origen.getNumeroCuenta(), origen.getNumeroCuenta(), "10.00")
                .statusCode(400)
                .body("codigo", equalTo("SOLICITUD_INVALIDA"));
    }

    @Test
    void montoCeroOConTresDecimalesDevuelve400() {
        Cuenta origen = cuenta("1000.00");
        Cuenta destino = cuenta("0.00");

        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "0")
                .statusCode(400)
                .body("codigo", equalTo("VALIDACION"))
                .body("detalles", hasItem(containsString("monto")));
        transferir(origen.getNumeroCuenta(), destino.getNumeroCuenta(), "10.555")
                .statusCode(400)
                .body("codigo", equalTo("VALIDACION"));
    }
}

package com.evaluacion.transferencias.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

class JsonInvalidoExceptionMapperTest {

    record SolicitudPrueba(TipoCuenta tipo) {
    }

    private final JsonInvalidoExceptionMapper mapper = new JsonInvalidoExceptionMapper();

    @Test
    void enumDesconocidoDevuelve400ConElCampoYLosValoresPermitidos() {
        MismatchedInputException exception = assertThrows(MismatchedInputException.class,
                () -> new ObjectMapper().readValue("{\"tipo\":\"PLAZO\"}", SolicitudPrueba.class));

        Response response = mapper.toResponse(exception);
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals("JSON_INVALIDO", error.codigo());
        assertEquals(List.of("tipo: valor 'PLAZO' no permitido; use AHORRO, CORRIENTE"), error.detalles());
    }
}

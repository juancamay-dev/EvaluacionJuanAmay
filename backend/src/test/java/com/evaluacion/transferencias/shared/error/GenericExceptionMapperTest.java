package com.evaluacion.transferencias.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;

class GenericExceptionMapperTest {

    private final GenericExceptionMapper mapper = new GenericExceptionMapper();

    @Test
    void webApplicationExceptionConservaSuStatus() {
        Response response = mapper.toResponse(new NotFoundException("Ruta inexistente"));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(404, response.getStatus());
        assertEquals("RECURSO_NO_ENCONTRADO", error.codigo());
    }

    @Test
    void jsonInvalidoDevuelve400() {
        Response response = mapper.toResponse(new JsonParseException(null, "Unexpected character"));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals("JSON_INVALIDO", error.codigo());
    }

    @Test
    void errorInesperadoDevuelve500SinExponerElMensajeInterno() {
        Response response = mapper.toResponse(new IllegalStateException("SQL interno: tabla x"));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("ERROR_INTERNO", error.codigo());
        assertFalse(error.mensaje().contains("SQL"));
    }
}

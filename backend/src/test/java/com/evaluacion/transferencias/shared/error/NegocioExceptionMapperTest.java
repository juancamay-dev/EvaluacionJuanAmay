package com.evaluacion.transferencias.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class NegocioExceptionMapperTest {

    private final NegocioExceptionMapper mapper = new NegocioExceptionMapper();

    @Test
    void mapeaStatusCodigoYMensaje() {
        Response response = mapper.toResponse(new RecursoNoEncontradoException("No existe la cuenta 1"));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(404, response.getStatus());
        assertEquals(404, error.status());
        assertEquals("RECURSO_NO_ENCONTRADO", error.codigo());
        assertEquals("No existe la cuenta 1", error.mensaje());
        assertTrue(error.detalles().isEmpty());
    }

    @Test
    void transferenciaRechazadaIncluyeIdEnDetalles() {
        Response response = mapper.toResponse(new TransferenciaRechazadaException(
                TransferenciaRechazadaException.SALDO_INSUFICIENTE, "Saldo insuficiente", 15L));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(422, response.getStatus());
        assertEquals("SALDO_INSUFICIENTE", error.codigo());
        assertEquals(List.of("transferenciaId: 15"), error.detalles());
    }

    @Test
    void respuestaEsJson() {
        Response response = mapper.toResponse(new CuentaDuplicadaException("1000000001"));

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(409, response.getStatus());
    }
}

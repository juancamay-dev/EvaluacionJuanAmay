package com.evaluacion.transferencias.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class ValidacionExceptionMapperTest {

    record SolicitudPrueba(@NotBlank String titular, @Positive BigDecimal monto) {
    }

    record Envoltura(@Valid SolicitudPrueba request) {
    }

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final ValidacionExceptionMapper mapper = new ValidacionExceptionMapper();

    private static ConstraintViolationException violaciones(Object objeto) {
        return new ConstraintViolationException(VALIDATOR.validate(objeto));
    }

    @Test
    void devuelve400ConCodigoValidacion() {
        Response response = mapper.toResponse(violaciones(new SolicitudPrueba("Ana", BigDecimal.ZERO)));
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals("VALIDACION", error.codigo());
    }

    @Test
    void detallesUsanSoloElNombreDelCampo() {
        Response response = mapper.toResponse(violaciones(new Envoltura(new SolicitudPrueba("Ana", BigDecimal.ZERO))));
        List<String> detalles = ((ErrorResponse) response.getEntity()).detalles();

        assertEquals(1, detalles.size());
        assertTrue(detalles.get(0).startsWith("monto: "), detalles.get(0));
    }

    @Test
    void detallesOrdenados() {
        Response response = mapper.toResponse(violaciones(new SolicitudPrueba(" ", new BigDecimal("-1"))));
        List<String> detalles = ((ErrorResponse) response.getEntity()).detalles();

        assertEquals(2, detalles.size());
        assertTrue(detalles.get(0).startsWith("monto: "));
        assertTrue(detalles.get(1).startsWith("titular: "));
    }
}

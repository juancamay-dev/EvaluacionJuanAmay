package com.evaluacion.transferencias.shared.error;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * Valores que Jackson no puede convertir (enum desconocido, texto en un campo numérico). Reemplaza
 * al mapper incorporado de Quarkus para responder con el formato de error de la API.
 */
@Provider
public class JsonInvalidoExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    @Override
    public Response toResponse(MismatchedInputException exception) {
        String campo = exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .reduce((primero, ultimo) -> ultimo)
                .orElse(null);
        List<String> detalles = campo == null ? List.of() : List.of(campo + ": " + descripcion(exception));
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(400, "JSON_INVALIDO", "El cuerpo de la solicitud contiene valores inválidos",
                        detalles))
                .build();
    }

    private static String descripcion(MismatchedInputException exception) {
        if (exception instanceof InvalidFormatException formato) {
            Class<?> tipo = formato.getTargetType();
            if (tipo != null && tipo.isEnum()) {
                List<String> permitidos = Arrays.stream(tipo.getEnumConstants()).map(Object::toString).toList();
                return "valor '" + formato.getValue() + "' no permitido; use " + String.join(", ", permitidos);
            }
            return "valor '" + formato.getValue() + "' con formato inválido";
        }
        return "tipo de dato inválido";
    }
}

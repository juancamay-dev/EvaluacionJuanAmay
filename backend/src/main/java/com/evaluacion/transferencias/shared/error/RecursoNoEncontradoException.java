package com.evaluacion.transferencias.shared.error;

public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(404, "RECURSO_NO_ENCONTRADO", mensaje);
    }
}

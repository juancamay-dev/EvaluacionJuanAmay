package com.evaluacion.transferencias.shared.error;

public class SolicitudInvalidaException extends NegocioException {

    public SolicitudInvalidaException(String mensaje) {
        super(400, "SOLICITUD_INVALIDA", mensaje);
    }
}

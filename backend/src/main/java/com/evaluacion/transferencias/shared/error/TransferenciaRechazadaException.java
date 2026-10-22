package com.evaluacion.transferencias.shared.error;

/** Rechazo por regla de negocio; la transferencia queda registrada como RECHAZADA. */
public class TransferenciaRechazadaException extends NegocioException {

    public static final String SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    public static final String CUENTA_INACTIVA = "CUENTA_INACTIVA";
    public static final String LIMITE_DIARIO_EXCEDIDO = "LIMITE_DIARIO_EXCEDIDO";

    private final Long transferenciaId;

    public TransferenciaRechazadaException(String codigo, String mensaje, Long transferenciaId) {
        super(422, codigo, mensaje);
        this.transferenciaId = transferenciaId;
    }

    public Long getTransferenciaId() {
        return transferenciaId;
    }
}

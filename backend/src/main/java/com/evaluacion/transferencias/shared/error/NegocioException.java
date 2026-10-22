package com.evaluacion.transferencias.shared.error;

/** Error de negocio con su status HTTP y un código estable para el cliente. */
public abstract class NegocioException extends RuntimeException {

    private final int status;
    private final String codigo;

    protected NegocioException(int status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}

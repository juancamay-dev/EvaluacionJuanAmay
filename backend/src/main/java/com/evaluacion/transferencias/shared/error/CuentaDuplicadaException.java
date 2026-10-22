package com.evaluacion.transferencias.shared.error;

public class CuentaDuplicadaException extends NegocioException {

    public CuentaDuplicadaException(String numeroCuenta) {
        super(409, "CUENTA_DUPLICADA", "Ya existe una cuenta con el número " + numeroCuenta);
    }
}

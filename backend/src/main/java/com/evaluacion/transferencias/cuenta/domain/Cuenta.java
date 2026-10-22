package com.evaluacion.transferencias.cuenta.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Cuenta {

    private Long id;
    private String numeroCuenta;
    private String titular;
    private TipoCuenta tipo;
    private BigDecimal saldo;
    private EstadoCuenta estado;
    private Instant fechaCreacion;

    protected Cuenta() {
    }

    public Cuenta(String numeroCuenta, String titular, TipoCuenta tipo, BigDecimal saldoInicial,
                  EstadoCuenta estado, Instant fechaCreacion) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void debitar(BigDecimal monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void acreditar(BigDecimal monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean tieneSaldoSuficiente(BigDecimal monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean estaActiva() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Long getId() {
        return id;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public String getTitular() {
        return titular;
    }

    public TipoCuenta getTipo() {
        return tipo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}

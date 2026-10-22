package com.evaluacion.transferencias.transferencia.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;

@Entity
@Table(name = "transferencia")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_origen_id", nullable = false)
    private Cuenta cuentaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_destino_id", nullable = false)
    private Cuenta cuentaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTransferencia estado;

    @Column(length = 255)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha;

    protected Transferencia() {
        // requerido por JPA
    }

    private Transferencia(Cuenta origen, Cuenta destino, BigDecimal monto, EstadoTransferencia estado,
                          String motivo, Instant fecha) {
        Objects.requireNonNull(origen, "La cuenta origen es obligatoria");
        Objects.requireNonNull(destino, "La cuenta destino es obligatoria");
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (origen.getNumeroCuenta().equals(destino.getNumeroCuenta())) {
            throw new IllegalArgumentException("La cuenta origen y la cuenta destino deben ser diferentes");
        }
        this.cuentaOrigen = origen;
        this.cuentaDestino = destino;
        this.monto = monto;
        this.estado = estado;
        this.motivo = motivo;
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria");
    }

    public static Transferencia completada(Cuenta origen, Cuenta destino, BigDecimal monto, Instant fecha) {
        return new Transferencia(origen, destino, monto, EstadoTransferencia.COMPLETADA, null, fecha);
    }

    public static Transferencia enRevision(Cuenta origen, Cuenta destino, BigDecimal monto, String motivo,
                                           Instant fecha) {
        return new Transferencia(origen, destino, monto, EstadoTransferencia.EN_REVISION, motivo, fecha);
    }

    public static Transferencia rechazada(Cuenta origen, Cuenta destino, BigDecimal monto, String motivo,
                                          Instant fecha) {
        return new Transferencia(origen, destino, monto, EstadoTransferencia.RECHAZADA, motivo, fecha);
    }

    public Long getId() {
        return id;
    }

    public Cuenta getCuentaOrigen() {
        return cuentaOrigen;
    }

    public Cuenta getCuentaDestino() {
        return cuentaDestino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public EstadoTransferencia getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getFecha() {
        return fecha;
    }
}

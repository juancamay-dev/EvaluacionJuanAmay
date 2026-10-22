package com.evaluacion.transferencias.cuenta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    private static final Pattern NUMERO_CUENTA = Pattern.compile("\\d{10}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 10)
    private String numeroCuenta;

    @Column(nullable = false, length = 120)
    private String titular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCuenta tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCuenta estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    protected Cuenta() {
        // requerido por JPA
    }

    public Cuenta(String numeroCuenta, String titular, TipoCuenta tipo, BigDecimal saldoInicial,
                  EstadoCuenta estado, Instant fechaCreacion) {
        if (numeroCuenta == null || !NUMERO_CUENTA.matcher(numeroCuenta).matches()) {
            throw new IllegalArgumentException("El número de cuenta debe tener exactamente 10 dígitos");
        }
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("El titular es obligatorio");
        }
        if (saldoInicial == null || saldoInicial.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        this.numeroCuenta = numeroCuenta;
        this.titular = titular;
        this.tipo = Objects.requireNonNull(tipo, "El tipo de cuenta es obligatorio");
        this.saldo = saldoInicial.setScale(2, RoundingMode.HALF_EVEN);
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "La fecha de creación es obligatoria");
    }

    public void debitar(BigDecimal monto) {
        validarMontoPositivo(monto);
        if (!tieneSaldoSuficiente(monto)) {
            throw new IllegalStateException("Saldo insuficiente en la cuenta " + numeroCuenta);
        }
        saldo = saldo.subtract(monto);
    }

    public void acreditar(BigDecimal monto) {
        validarMontoPositivo(monto);
        saldo = saldo.add(monto);
    }

    public boolean tieneSaldoSuficiente(BigDecimal monto) {
        return saldo.compareTo(monto) >= 0;
    }

    public boolean estaActiva() {
        return estado == EstadoCuenta.ACTIVA;
    }

    private static void validarMontoPositivo(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
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

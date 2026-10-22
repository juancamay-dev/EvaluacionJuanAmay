package com.evaluacion.transferencias.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.domain.EstadoCuenta;
import com.evaluacion.transferencias.cuenta.domain.TipoCuenta;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;

/** Persiste datos de prueba; se une a la transacción de la prueba si existe. */
@ApplicationScoped
public class DatosPrueba {

    public static final Instant AHORA = Instant.parse("2026-10-21T15:00:00Z");

    private static final AtomicLong SECUENCIA = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L);

    private final EntityManager entityManager;

    DatosPrueba(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Número de cuenta de 10 dígitos que no se repite entre pruebas. */
    public static String numeroUnico() {
        return "9" + String.format("%09d", SECUENCIA.incrementAndGet() % 1_000_000_000L);
    }

    @Transactional
    public Cuenta cuenta(String numero, String saldo) {
        return cuenta(numero, saldo, EstadoCuenta.ACTIVA);
    }

    @Transactional
    public Cuenta cuenta(String numero, String saldo, EstadoCuenta estado) {
        Cuenta cuenta = new Cuenta(numero, "Titular " + numero, TipoCuenta.AHORRO, new BigDecimal(saldo), estado,
                AHORA);
        entityManager.persist(cuenta);
        entityManager.flush();
        return cuenta;
    }

    @Transactional
    public Transferencia transferencia(Cuenta origen, Cuenta destino, String monto, EstadoTransferencia estado,
                                       Instant fecha) {
        BigDecimal valor = new BigDecimal(monto);
        Transferencia transferencia = switch (estado) {
            case COMPLETADA -> Transferencia.completada(origen, destino, valor, fecha);
            case EN_REVISION -> Transferencia.enRevision(origen, destino, valor, "Prueba", fecha);
            case RECHAZADA -> Transferencia.rechazada(origen, destino, valor, "Prueba", fecha);
        };
        entityManager.persist(transferencia);
        entityManager.flush();
        return transferencia;
    }
}

package com.evaluacion.transferencias.cuenta.infrastructure;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class CuentaRepository implements PanacheRepository<Cuenta> {

    public boolean existePorNumero(String numeroCuenta) {
        return count("numeroCuenta", numeroCuenta) > 0;
    }

    /** SELECT ... FOR UPDATE: bloquea la fila hasta el fin de la transacción. */
    public Optional<Cuenta> buscarPorNumeroParaActualizar(String numeroCuenta) {
        return find("numeroCuenta", numeroCuenta)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }

    public PanacheQuery<Cuenta> listarOrdenadas() {
        return findAll(Sort.ascending("numeroCuenta"));
    }
}

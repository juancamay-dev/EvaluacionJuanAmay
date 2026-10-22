package com.evaluacion.transferencias.cuenta.application;

import java.time.Instant;

import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.cuenta.domain.Cuenta;

final class CuentaMapper {

    private CuentaMapper() {
    }

    static Cuenta toEntity(CuentaRequest request, Instant fechaCreacion) {
        return new Cuenta(request.numeroCuenta(), request.titular().trim(), request.tipo(), request.saldoInicial(),
                request.estado(), fechaCreacion);
    }

    static CuentaResponse toResponse(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getTitular(), cuenta.getTipo(),
                cuenta.getSaldo(), cuenta.getEstado(), cuenta.getFechaCreacion());
    }
}

package com.evaluacion.transferencias.transferencia.application;

import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaResponse;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;

final class TransferenciaMapper {

    private TransferenciaMapper() {
    }

    static TransferenciaResponse toResponse(Transferencia transferencia) {
        return new TransferenciaResponse(transferencia.getId(), transferencia.getCuentaOrigen().getNumeroCuenta(),
                transferencia.getCuentaDestino().getNumeroCuenta(), transferencia.getMonto(),
                transferencia.getEstado(), transferencia.getMotivo(), transferencia.getFecha());
    }
}

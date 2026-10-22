package com.evaluacion.transferencias.transferencia.domain;

import java.util.List;

public record ResultadoDeteccion(List<String> motivos) {

    public ResultadoDeteccion {
        motivos = List.copyOf(motivos);
    }

    public boolean esSospechosa() {
        return !motivos.isEmpty();
    }

    public String motivo() {
        return esSospechosa() ? String.join("; ", motivos) : null;
    }
}

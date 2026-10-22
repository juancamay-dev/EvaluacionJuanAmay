package com.evaluacion.transferencias.shared.pagination;

import java.util.List;

public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <T> PaginaResponse<T> of(List<T> contenido, int pagina, int tamano, long total) {
        int totalPaginas = (int) ((total + tamano - 1) / tamano);
        return new PaginaResponse<>(List.copyOf(contenido), pagina, tamano, total, totalPaginas);
    }
}

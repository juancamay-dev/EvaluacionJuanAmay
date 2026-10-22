package com.evaluacion.transferencias.shared.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaginaResponseTest {

    @Test
    void totalPaginasRedondeaHaciaArriba() {
        PaginaResponse<String> pagina = PaginaResponse.of(List.of("a"), 1, 20, 21);

        assertEquals(2, pagina.totalPaginas());
        assertEquals(21, pagina.totalElementos());
        assertEquals(1, pagina.pagina());
        assertEquals(20, pagina.tamano());
    }

    @Test
    void sinElementosTieneCeroPaginas() {
        PaginaResponse<String> pagina = PaginaResponse.of(List.of(), 0, 20, 0);

        assertEquals(0, pagina.totalPaginas());
        assertEquals(List.of(), pagina.contenido());
    }
}

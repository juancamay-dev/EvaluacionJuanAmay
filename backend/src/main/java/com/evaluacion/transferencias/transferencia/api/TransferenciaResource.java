package com.evaluacion.transferencias.transferencia.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.evaluacion.transferencias.shared.pagination.PaginaResponse;
import com.evaluacion.transferencias.transferencia.application.TransferenciaService;
import com.evaluacion.transferencias.transferencia.application.dto.HistorialFiltro;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaRequest;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaResponse;

@Path("/api/transferencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransferenciaResource {

    private final TransferenciaService transferenciaService;

    @Inject
    public TransferenciaResource(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @POST
    public Response ejecutar(@NotNull(message = "el cuerpo es obligatorio") @Valid TransferenciaRequest request) {
        return Response.status(Response.Status.CREATED).entity(transferenciaService.ejecutar(request)).build();
    }

    @GET
    public PaginaResponse<TransferenciaResponse> historial(
            @QueryParam("cuentaOrigen") @Pattern(regexp = "\\d{10}", message = "debe tener 10 dígitos")
            String cuentaOrigen,
            @QueryParam("cuentaDestino") @Pattern(regexp = "\\d{10}", message = "debe tener 10 dígitos")
            String cuentaDestino,
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta,
            @QueryParam("estado") String estado,
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @QueryParam("tamano") @DefaultValue("20") @Min(1) @Max(500) int tamano) {
        return transferenciaService.historial(new HistorialFiltro(cuentaOrigen, cuentaDestino, desde, hasta, estado),
                pagina, tamano);
    }
}

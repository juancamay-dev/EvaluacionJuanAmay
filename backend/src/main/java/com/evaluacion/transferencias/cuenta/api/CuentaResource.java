package com.evaluacion.transferencias.cuenta.api;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.evaluacion.transferencias.cuenta.application.CuentaService;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.shared.pagination.PaginaResponse;

@Path("/api/cuentas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CuentaResource {

    private final CuentaService cuentaService;

    @Inject
    public CuentaResource(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @POST
    public Response crear(@NotNull(message = "el cuerpo es obligatorio") @Valid CuentaRequest request,
                          @Context UriInfo uriInfo) {
        CuentaResponse cuenta = cuentaService.crear(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(cuenta.id())).build();
        return Response.created(location).entity(cuenta).build();
    }

    @GET
    public PaginaResponse<CuentaResponse> listar(
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @QueryParam("tamano") @DefaultValue("100") @Min(1) @Max(500) int tamano) {
        return cuentaService.listar(pagina, tamano);
    }

    @GET
    @Path("/{id}")
    public CuentaResponse obtener(@PathParam("id") Long id) {
        return cuentaService.obtener(id);
    }
}

package com.evaluacion.transferencias.transferencia.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.evaluacion.transferencias.transferencia.application.TransferenciaService;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaRequest;

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
}

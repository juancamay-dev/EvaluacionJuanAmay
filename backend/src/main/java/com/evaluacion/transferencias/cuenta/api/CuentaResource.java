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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.evaluacion.transferencias.cuenta.application.CuentaService;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.cuenta.application.dto.ResumenCuentaResponse;
import com.evaluacion.transferencias.shared.error.ErrorResponse;
import com.evaluacion.transferencias.shared.pagination.PaginaResponse;

@Path("/api/cuentas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Cuentas", description = "Registro y consulta de cuentas bancarias")
public class CuentaResource {

    private final CuentaService cuentaService;

    @Inject
    public CuentaResource(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @POST
    @Operation(summary = "Registrar una cuenta bancaria",
            description = "Crea una cuenta con número único de 10 dígitos y saldo inicial mayor o igual a cero. "
                    + "Tipo y estado se aceptan en mayúsculas o minúsculas.")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = CuentaRequest.class),
            examples = @ExampleObject(name = "ahorro", summary = "Cuenta de ahorro activa",
                    value = "{\"numeroCuenta\":\"1000000010\",\"titular\":\"Ana Pérez\",\"tipo\":\"AHORRO\","
                            + "\"saldoInicial\":1500.00,\"estado\":\"ACTIVA\"}")))
    @APIResponse(responseCode = "201", description = "Cuenta creada; la cabecera Location apunta a la cuenta",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CuentaResponse.class)))
    @APIResponse(responseCode = "400", description = "Datos inválidos o JSON mal formado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Ya existe una cuenta con ese número",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public Response crear(@NotNull(message = "el cuerpo es obligatorio") @Valid CuentaRequest request,
                          @Context UriInfo uriInfo) {
        CuentaResponse cuenta = cuentaService.crear(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(cuenta.id())).build();
        return Response.created(location).entity(cuenta).build();
    }

    @GET
    @Operation(summary = "Listar las cuentas registradas",
            description = "Cuentas ordenadas por número. Por defecto devuelve hasta 100 por página.")
    @APIResponse(responseCode = "200", description = "Página de cuentas")
    @APIResponse(responseCode = "400", description = "Parámetros de paginación inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<CuentaResponse> listar(
            @Parameter(description = "Número de página, desde 0")
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @Parameter(description = "Elementos por página (1 a 500)")
            @QueryParam("tamano") @DefaultValue("100") @Min(1) @Max(500) int tamano) {
        return cuentaService.listar(pagina, tamano);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar una cuenta por id")
    @APIResponse(responseCode = "200", description = "Cuenta encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CuentaResponse.class)))
    @APIResponse(responseCode = "404", description = "La cuenta no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public CuentaResponse obtener(@Parameter(description = "Id de la cuenta", example = "1") @PathParam("id") Long id) {
        return cuentaService.obtener(id);
    }

    @GET
    @Path("/{id}/resumen")
    @Operation(summary = "Resumen de la cuenta en tiempo real",
            description = "Saldo actual, total transferido hoy, cantidad de transferencias del día, cantidad "
                    + "marcadas como sospechosas y monto disponible frente al límite diario.")
    @APIResponse(responseCode = "200", description = "Resumen del día",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ResumenCuentaResponse.class)))
    @APIResponse(responseCode = "404", description = "La cuenta no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public ResumenCuentaResponse resumen(@Parameter(description = "Id de la cuenta", example = "1")
                                         @PathParam("id") Long id) {
        return cuentaService.resumen(id);
    }
}

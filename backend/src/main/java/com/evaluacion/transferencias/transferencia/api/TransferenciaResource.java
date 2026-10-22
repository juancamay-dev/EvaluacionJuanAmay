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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.evaluacion.transferencias.shared.error.ErrorResponse;
import com.evaluacion.transferencias.shared.pagination.PaginaResponse;
import com.evaluacion.transferencias.transferencia.application.TransferenciaService;
import com.evaluacion.transferencias.transferencia.application.dto.HistorialFiltro;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaRequest;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaResponse;

@Path("/api/transferencias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transferencias", description = "Ejecución de transferencias e historial")
public class TransferenciaResource {

    private final TransferenciaService transferenciaService;

    @Inject
    public TransferenciaResource(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @POST
    @Operation(summary = "Ejecutar una transferencia",
            description = "Debita la cuenta origen y acredita la destino en una sola transacción. Valida que ambas "
                    + "cuentas existan y estén activas, que haya saldo suficiente y que no se supere el límite "
                    + "diario de $10,000. Si el monto supera $5,000 o la cuenta origen hizo más de 5 "
                    + "transferencias en la última hora, se ejecuta y queda EN_REVISION. Los rechazos de negocio "
                    + "quedan registrados como RECHAZADA.")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = TransferenciaRequest.class),
            examples = {
                    @ExampleObject(name = "valida", summary = "Transferencia válida",
                            value = "{\"cuentaOrigen\":\"1000000001\",\"cuentaDestino\":\"1000000002\",\"monto\":250.00}"),
                    @ExampleObject(name = "sospechosa", summary = "Monto mayor a $5,000 (queda EN_REVISION)",
                            value = "{\"cuentaOrigen\":\"1000000001\",\"cuentaDestino\":\"1000000003\",\"monto\":6000.00}")
            }))
    @APIResponse(responseCode = "201", description = "Transferencia ejecutada (COMPLETADA o EN_REVISION)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = TransferenciaResponse.class)))
    @APIResponse(responseCode = "400", description = "Datos inválidos o cuenta origen igual a la destino",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "La cuenta origen o destino no existe",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "422",
            description = "Rechazada por regla de negocio: CUENTA_INACTIVA, SALDO_INSUFICIENTE o "
                    + "LIMITE_DIARIO_EXCEDIDO (el mensaje informa el monto disponible)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "limite", value = "{\"timestamp\":\"2026-10-21T15:00:00Z\","
                            + "\"status\":422,\"codigo\":\"LIMITE_DIARIO_EXCEDIDO\",\"mensaje\":\"La transferencia "
                            + "supera el límite diario de $10,000.00. Monto disponible hoy: $1,000.00\","
                            + "\"detalles\":[\"transferenciaId: 15\"]}")))
    public Response ejecutar(@NotNull(message = "el cuerpo es obligatorio") @Valid TransferenciaRequest request) {
        return Response.status(Response.Status.CREATED).entity(transferenciaService.ejecutar(request)).build();
    }

    @GET
    @Operation(summary = "Consultar el historial de transferencias",
            description = "Filtros opcionales y combinables. Resultados paginados y ordenados por fecha descendente.")
    @APIResponse(responseCode = "200", description = "Página de transferencias")
    @APIResponse(responseCode = "400", description = "Filtros inválidos (estado, fechas o número de cuenta)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<TransferenciaResponse> historial(
            @Parameter(description = "Número de la cuenta origen (10 dígitos)", example = "1000000001")
            @QueryParam("cuentaOrigen") @Pattern(regexp = "\\d{10}", message = "debe tener 10 dígitos")
            String cuentaOrigen,
            @Parameter(description = "Número de la cuenta destino (10 dígitos)", example = "1000000002")
            @QueryParam("cuentaDestino") @Pattern(regexp = "\\d{10}", message = "debe tener 10 dígitos")
            String cuentaDestino,
            @Parameter(description = "Fecha inicial inclusiva (yyyy-MM-dd, hora de Ecuador)", example = "2026-10-01")
            @QueryParam("desde") String desde,
            @Parameter(description = "Fecha final inclusiva (yyyy-MM-dd, hora de Ecuador)", example = "2026-10-31")
            @QueryParam("hasta") String hasta,
            @Parameter(description = "Estado de la transferencia",
                    schema = @Schema(type = SchemaType.STRING,
                            enumeration = {"COMPLETADA", "RECHAZADA", "EN_REVISION"}))
            @QueryParam("estado") String estado,
            @Parameter(description = "Número de página, desde 0")
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @Parameter(description = "Elementos por página (1 a 500)")
            @QueryParam("tamano") @DefaultValue("20") @Min(1) @Max(500) int tamano) {
        return transferenciaService.historial(new HistorialFiltro(cuentaOrigen, cuentaDestino, desde, hasta, estado),
                pagina, tamano);
    }
}

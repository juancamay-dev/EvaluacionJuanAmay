package com.evaluacion.transferencias.cuenta.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.cuenta.application.dto.ResumenCuentaResponse;
import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.infrastructure.CuentaRepository;
import com.evaluacion.transferencias.shared.error.CuentaDuplicadaException;
import com.evaluacion.transferencias.shared.error.RecursoNoEncontradoException;
import com.evaluacion.transferencias.shared.pagination.PaginaResponse;
import com.evaluacion.transferencias.shared.time.RangoDia;
import com.evaluacion.transferencias.transferencia.application.LimiteDiarioService;
import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;

@ApplicationScoped
public class CuentaService {

    private static final Logger LOG = Logger.getLogger(CuentaService.class);

    private final CuentaRepository cuentaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final LimiteDiarioService limiteDiarioService;
    private final Clock reloj;

    @Inject
    public CuentaService(CuentaRepository cuentaRepository, TransferenciaRepository transferenciaRepository,
                         LimiteDiarioService limiteDiarioService, Clock reloj) {
        this.cuentaRepository = cuentaRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.limiteDiarioService = limiteDiarioService;
        this.reloj = reloj;
    }

    @Transactional
    public CuentaResponse crear(CuentaRequest request) {
        if (cuentaRepository.existePorNumero(request.numeroCuenta())) {
            throw new CuentaDuplicadaException(request.numeroCuenta());
        }
        Cuenta cuenta = CuentaMapper.toEntity(request, reloj.instant());
        try {
            cuentaRepository.persistAndFlush(cuenta);
        } catch (ConstraintViolationException e) {
            // Otra solicitud registró el mismo número entre la verificación y el insert
            throw new CuentaDuplicadaException(request.numeroCuenta());
        }
        LOG.infof("cuenta.creada id=%d numero=%s tipo=%s estado=%s", cuenta.getId(), cuenta.getNumeroCuenta(),
                cuenta.getTipo(), cuenta.getEstado());
        return CuentaMapper.toResponse(cuenta);
    }

    public PaginaResponse<CuentaResponse> listar(int pagina, int tamano) {
        PanacheQuery<Cuenta> consulta = cuentaRepository.listarOrdenadas().page(pagina, tamano);
        List<CuentaResponse> contenido = consulta.list().stream().map(CuentaMapper::toResponse).toList();
        return PaginaResponse.of(contenido, pagina, tamano, consulta.count());
    }

    public CuentaResponse obtener(Long id) {
        return CuentaMapper.toResponse(buscar(id));
    }

    /** Resumen calculado contra la base de datos en cada consulta (día calendario del negocio). */
    public ResumenCuentaResponse resumen(Long id) {
        Cuenta cuenta = buscar(id);
        Instant ahora = reloj.instant();
        RangoDia hoy = RangoDia.de(ahora, reloj.getZone());
        BigDecimal transferidoHoy = limiteDiarioService.acumuladoDelDia(cuenta.getId(), ahora);
        BigDecimal limite = limiteDiarioService.getLimiteDiario();
        return new ResumenCuentaResponse(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getTitular(), hoy.fecha(),
                cuenta.getSaldo(), transferidoHoy,
                transferenciaRepository.contarRealizadasEntre(cuenta.getId(), hoy.inicio(), hoy.fin()),
                transferenciaRepository.contarPorEstadoEntre(cuenta.getId(), EstadoTransferencia.EN_REVISION,
                        hoy.inicio(), hoy.fin()),
                transferenciaRepository.contarPorEstado(cuenta.getId(), EstadoTransferencia.EN_REVISION),
                limite, limite.subtract(transferidoHoy).max(BigDecimal.ZERO));
    }

    private Cuenta buscar(Long id) {
        return cuentaRepository.findByIdOptional(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta con id " + id));
    }
}

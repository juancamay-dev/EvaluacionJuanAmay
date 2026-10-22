package com.evaluacion.transferencias.cuenta.application;

import java.time.Clock;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import com.evaluacion.transferencias.cuenta.application.dto.CuentaRequest;
import com.evaluacion.transferencias.cuenta.application.dto.CuentaResponse;
import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.infrastructure.CuentaRepository;
import com.evaluacion.transferencias.shared.error.CuentaDuplicadaException;
import com.evaluacion.transferencias.shared.error.RecursoNoEncontradoException;
import com.evaluacion.transferencias.shared.pagination.PaginaResponse;

import io.quarkus.hibernate.orm.panache.PanacheQuery;

@ApplicationScoped
public class CuentaService {

    private static final Logger LOG = Logger.getLogger(CuentaService.class);

    private final CuentaRepository cuentaRepository;
    private final Clock reloj;

    @Inject
    public CuentaService(CuentaRepository cuentaRepository, Clock reloj) {
        this.cuentaRepository = cuentaRepository;
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
        return cuentaRepository.findByIdOptional(id)
                .map(CuentaMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta con id " + id));
    }
}

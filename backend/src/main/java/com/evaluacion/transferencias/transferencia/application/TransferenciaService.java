package com.evaluacion.transferencias.transferencia.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.cuenta.infrastructure.CuentaRepository;
import com.evaluacion.transferencias.shared.error.RecursoNoEncontradoException;
import com.evaluacion.transferencias.shared.error.SolicitudInvalidaException;
import com.evaluacion.transferencias.shared.error.TransferenciaRechazadaException;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaRequest;
import com.evaluacion.transferencias.transferencia.application.dto.TransferenciaResponse;
import com.evaluacion.transferencias.transferencia.domain.EvaluacionLimite;
import com.evaluacion.transferencias.transferencia.domain.ResultadoDeteccion;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

@ApplicationScoped
public class TransferenciaService {

    private static final Logger LOG = Logger.getLogger(TransferenciaService.class);

    private final CuentaRepository cuentaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final LimiteDiarioService limiteDiarioService;
    private final DeteccionSospechosaService deteccionSospechosaService;
    private final Clock reloj;

    @Inject
    public TransferenciaService(CuentaRepository cuentaRepository, TransferenciaRepository transferenciaRepository,
                                LimiteDiarioService limiteDiarioService,
                                DeteccionSospechosaService deteccionSospechosaService, Clock reloj) {
        this.cuentaRepository = cuentaRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.limiteDiarioService = limiteDiarioService;
        this.deteccionSospechosaService = deteccionSospechosaService;
        this.reloj = reloj;
    }

    /**
     * Débito, crédito y registro en una sola transacción. Los rechazos de negocio no revierten la
     * transacción: la transferencia queda guardada como RECHAZADA para auditoría y se responde 422.
     */
    @Transactional(dontRollbackOn = TransferenciaRechazadaException.class)
    public TransferenciaResponse ejecutar(TransferenciaRequest request) {
        if (request.cuentaOrigen().equals(request.cuentaDestino())) {
            throw new SolicitudInvalidaException("La cuenta origen y la cuenta destino deben ser diferentes");
        }
        Instant ahora = reloj.instant();
        CuentasBloqueadas cuentas = bloquearEnOrden(request.cuentaOrigen(), request.cuentaDestino());
        validarReglas(cuentas.origen(), cuentas.destino(), request.monto(), ahora);

        ResultadoDeteccion deteccion = deteccionSospechosaService.evaluar(cuentas.origen(), request.monto(), ahora);
        cuentas.origen().debitar(request.monto());
        cuentas.destino().acreditar(request.monto());
        Transferencia transferencia = deteccion.esSospechosa()
                ? Transferencia.enRevision(cuentas.origen(), cuentas.destino(), request.monto(), deteccion.motivo(),
                ahora)
                : Transferencia.completada(cuentas.origen(), cuentas.destino(), request.monto(), ahora);
        transferenciaRepository.persist(transferencia);

        LOG.infof("transferencia.ejecutada id=%d origen=%s destino=%s monto=%s estado=%s", transferencia.getId(),
                request.cuentaOrigen(), request.cuentaDestino(), request.monto(), transferencia.getEstado());
        return TransferenciaMapper.toResponse(transferencia);
    }

    /**
     * Bloquea ambas cuentas (SELECT ... FOR UPDATE) siempre en orden ascendente de número: dos
     * transferencias cruzadas A->B y B->A piden los bloqueos en el mismo orden y no se bloquean
     * mutuamente.
     */
    private CuentasBloqueadas bloquearEnOrden(String numeroOrigen, String numeroDestino) {
        boolean origenPrimero = numeroOrigen.compareTo(numeroDestino) < 0;
        Cuenta primera = bloquear(origenPrimero ? numeroOrigen : numeroDestino);
        Cuenta segunda = bloquear(origenPrimero ? numeroDestino : numeroOrigen);
        return origenPrimero ? new CuentasBloqueadas(primera, segunda) : new CuentasBloqueadas(segunda, primera);
    }

    private Cuenta bloquear(String numeroCuenta) {
        return cuentaRepository.buscarPorNumeroParaActualizar(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cuenta " + numeroCuenta));
    }

    private void validarReglas(Cuenta origen, Cuenta destino, BigDecimal monto, Instant ahora) {
        if (!origen.estaActiva()) {
            rechazar(origen, destino, monto, ahora, TransferenciaRechazadaException.CUENTA_INACTIVA,
                    "La cuenta origen " + origen.getNumeroCuenta() + " está inactiva");
        }
        if (!destino.estaActiva()) {
            rechazar(origen, destino, monto, ahora, TransferenciaRechazadaException.CUENTA_INACTIVA,
                    "La cuenta destino " + destino.getNumeroCuenta() + " está inactiva");
        }
        if (!origen.tieneSaldoSuficiente(monto)) {
            rechazar(origen, destino, monto, ahora, TransferenciaRechazadaException.SALDO_INSUFICIENTE,
                    "Saldo insuficiente en la cuenta " + origen.getNumeroCuenta() + ". Saldo disponible: "
                            + dinero(origen.getSaldo()));
        }
        EvaluacionLimite limite = limiteDiarioService.evaluar(origen.getId(), monto, ahora);
        if (limite.excedido()) {
            rechazar(origen, destino, monto, ahora, TransferenciaRechazadaException.LIMITE_DIARIO_EXCEDIDO,
                    "La transferencia supera el límite diario de " + dinero(limite.limite())
                            + ". Monto disponible hoy: " + dinero(limite.disponible()));
        }
    }

    private void rechazar(Cuenta origen, Cuenta destino, BigDecimal monto, Instant ahora, String codigo,
                          String mensaje) {
        Transferencia rechazada = Transferencia.rechazada(origen, destino, monto, mensaje, ahora);
        transferenciaRepository.persist(rechazada);
        LOG.warnf("transferencia.rechazada id=%d origen=%s destino=%s monto=%s codigo=%s", rechazada.getId(),
                origen.getNumeroCuenta(), destino.getNumeroCuenta(), monto, codigo);
        throw new TransferenciaRechazadaException(codigo, mensaje, rechazada.getId());
    }

    private static String dinero(BigDecimal monto) {
        return String.format(Locale.US, "$%,.2f", monto);
    }

    private record CuentasBloqueadas(Cuenta origen, Cuenta destino) {
    }
}

package com.evaluacion.transferencias.transferencia.application;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.evaluacion.transferencias.cuenta.domain.Cuenta;
import com.evaluacion.transferencias.transferencia.domain.ResultadoDeteccion;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

/** CA-03: marca para revisión los montos altos y la frecuencia inusual. */
@ApplicationScoped
public class DeteccionSospechosaService {

    private static final Logger LOG = Logger.getLogger(DeteccionSospechosaService.class);
    private static final Duration VENTANA = Duration.ofHours(1);

    private final TransferenciaRepository transferenciaRepository;
    private final BigDecimal montoSospechoso;
    private final int maxTransferenciasPorHora;

    @Inject
    public DeteccionSospechosaService(TransferenciaRepository transferenciaRepository,
                                      @ConfigProperty(name = "app.reglas.monto-sospechoso") BigDecimal montoSospechoso,
                                      @ConfigProperty(name = "app.reglas.max-transferencias-por-hora")
                                      int maxTransferenciasPorHora) {
        this.transferenciaRepository = transferenciaRepository;
        this.montoSospechoso = montoSospechoso;
        this.maxTransferenciasPorHora = maxTransferenciasPorHora;
    }

    public ResultadoDeteccion evaluar(Cuenta origen, BigDecimal monto, Instant instante) {
        List<String> motivos = new ArrayList<>();
        if (monto.compareTo(montoSospechoso) > 0) {
            motivos.add(String.format(Locale.US, "Monto superior a $%,.2f", montoSospechoso));
        }
        long previas = transferenciaRepository.contarRealizadasEntre(origen.getId(), instante.minus(VENTANA),
                instante);
        if (previas >= maxTransferenciasPorHora) {
            motivos.add("Más de " + maxTransferenciasPorHora + " transferencias en la última hora");
        }
        ResultadoDeteccion resultado = new ResultadoDeteccion(motivos);
        if (resultado.esSospechosa()) {
            LOG.warnf("fraude.sospecha cuenta=%s monto=%s previasUltimaHora=%d motivos=\"%s\"",
                    origen.getNumeroCuenta(), monto, previas, resultado.motivo());
        }
        return resultado;
    }
}

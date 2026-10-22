package com.evaluacion.transferencias.transferencia.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.evaluacion.transferencias.shared.time.RangoDia;
import com.evaluacion.transferencias.transferencia.domain.EvaluacionLimite;
import com.evaluacion.transferencias.transferencia.infrastructure.TransferenciaRepository;

/** CA-04: ninguna cuenta transfiere más del límite acumulado en un día calendario. */
@ApplicationScoped
public class LimiteDiarioService {

    private final TransferenciaRepository transferenciaRepository;
    private final BigDecimal limiteDiario;
    private final ZoneId zonaHoraria;

    @Inject
    public LimiteDiarioService(TransferenciaRepository transferenciaRepository,
                               @ConfigProperty(name = "app.reglas.limite-diario") BigDecimal limiteDiario,
                               @ConfigProperty(name = "app.zona-horaria") ZoneId zonaHoraria) {
        this.transferenciaRepository = transferenciaRepository;
        this.limiteDiario = limiteDiario;
        this.zonaHoraria = zonaHoraria;
    }

    public EvaluacionLimite evaluar(Long cuentaId, BigDecimal monto, Instant instante) {
        BigDecimal acumulado = acumuladoDelDia(cuentaId, instante);
        BigDecimal disponible = limiteDiario.subtract(acumulado).max(BigDecimal.ZERO);
        return new EvaluacionLimite(limiteDiario, acumulado, disponible, monto.compareTo(disponible) > 0);
    }

    public BigDecimal acumuladoDelDia(Long cuentaId, Instant instante) {
        RangoDia dia = RangoDia.de(instante, zonaHoraria);
        return transferenciaRepository.sumarRealizadasEntre(cuentaId, dia.inicio(), dia.fin());
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }
}

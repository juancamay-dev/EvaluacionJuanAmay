package com.evaluacion.transferencias.transferencia.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import com.evaluacion.transferencias.transferencia.domain.EstadoTransferencia;
import com.evaluacion.transferencias.transferencia.domain.Transferencia;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;

@ApplicationScoped
public class TransferenciaRepository implements PanacheRepository<Transferencia> {

    /** Estados que mueven dinero: cuentan para el límite diario y la frecuencia. */
    public static final List<EstadoTransferencia> REALIZADAS =
            List.of(EstadoTransferencia.COMPLETADA, EstadoTransferencia.EN_REVISION);

    public BigDecimal sumarRealizadasEntre(Long cuentaOrigenId, Instant desde, Instant hasta) {
        BigDecimal total = getEntityManager().createQuery("""
                        select sum(t.monto) from Transferencia t
                        where t.cuentaOrigen.id = :cuenta and t.estado in :estados
                          and t.fecha >= :desde and t.fecha < :hasta""", BigDecimal.class)
                .setParameter("cuenta", cuentaOrigenId)
                .setParameter("estados", REALIZADAS)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }

    public long contarRealizadasEntre(Long cuentaOrigenId, Instant desde, Instant hasta) {
        return count("cuentaOrigen.id = ?1 and estado in ?2 and fecha >= ?3 and fecha < ?4",
                cuentaOrigenId, REALIZADAS, desde, hasta);
    }

    public long contarPorEstadoEntre(Long cuentaOrigenId, EstadoTransferencia estado, Instant desde,
                                     Instant hasta) {
        return count("cuentaOrigen.id = ?1 and estado = ?2 and fecha >= ?3 and fecha < ?4",
                cuentaOrigenId, estado, desde, hasta);
    }

    public long contarPorEstado(Long cuentaOrigenId, EstadoTransferencia estado) {
        return count("cuentaOrigen.id = ?1 and estado = ?2", cuentaOrigenId, estado);
    }

    public PanacheQuery<Transferencia> buscar(FiltroTransferencias filtro) {
        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();
        if (filtro.cuentaOrigen() != null) {
            condiciones.add("cuentaOrigen.numeroCuenta = :origen");
            parametros.put("origen", filtro.cuentaOrigen());
        }
        if (filtro.cuentaDestino() != null) {
            condiciones.add("cuentaDestino.numeroCuenta = :destino");
            parametros.put("destino", filtro.cuentaDestino());
        }
        if (filtro.estado() != null) {
            condiciones.add("estado = :estado");
            parametros.put("estado", filtro.estado());
        }
        if (filtro.desde() != null) {
            condiciones.add("fecha >= :desde");
            parametros.put("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            condiciones.add("fecha < :hasta");
            parametros.put("hasta", filtro.hasta());
        }
        Sort orden = Sort.descending("fecha", "id");
        return condiciones.isEmpty()
                ? findAll(orden)
                : find(String.join(" and ", condiciones), orden, parametros);
    }
}

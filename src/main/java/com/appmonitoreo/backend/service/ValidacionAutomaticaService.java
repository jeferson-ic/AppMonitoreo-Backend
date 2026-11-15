package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.model.ValidacionReporte;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.ValidacionReporteRepository;
import com.appmonitoreo.backend.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementa RF10 / RNF07: valida automáticamente un incidente cuando
 * usuarios distintos (3 o más) reportan el mismo tipo de incidente dentro de
 * un radio de 150 m y una ventana de 24 horas. Al cumplirse el consenso se
 * validan todos los reportes pendientes del grupo.
 */
@Service
public class ValidacionAutomaticaService {

    static final double RADIO_METROS = 150;
    static final int VENTANA_HORAS = 24;
    static final int MIN_REPORTES = 3;
    private static final long UMBRAL_MS_RNF07 = 5000;

    private final IncidenteRepository incidenteRepository;
    private final ValidacionReporteRepository validacionReporteRepository;
    private final EventoLogService eventoLogService;
    private final AlertaService alertaService;

    public ValidacionAutomaticaService(IncidenteRepository incidenteRepository,
                                       ValidacionReporteRepository validacionReporteRepository,
                                       EventoLogService eventoLogService,
                                       AlertaService alertaService) {
        this.incidenteRepository = incidenteRepository;
        this.validacionReporteRepository = validacionReporteRepository;
        this.eventoLogService = eventoLogService;
        this.alertaService = alertaService;
    }

    @Transactional
    public boolean evaluarYValidar(Incidente incidente) {
        long inicio = System.currentTimeMillis();
        try {
            return evaluar(incidente);
        } finally {
            long duracionMs = System.currentTimeMillis() - inicio;
            if (duracionMs > UMBRAL_MS_RNF07) {
                eventoLogService.warning("RNF07_VALIDACION_AUTOMATICA",
                        "La evaluación del incidente " + incidente.getIdIncidente() + " tardó " + duracionMs +
                                " ms (umbral RNF07: " + UMBRAL_MS_RNF07 + " ms)");
            }
        }
    }

    private boolean evaluar(Incidente incidente) {
        if (!"PENDIENTE".equals(incidente.getEstado())) {
            return false;
        }

        double lat = incidente.getLatitud().doubleValue();
        double lng = incidente.getLongitud().doubleValue();
        LocalDateTime desde = incidente.getFechaIncidente().minusHours(VENTANA_HORAS);

        double deltaLat = RADIO_METROS / 111_000.0;
        double deltaLng = RADIO_METROS / (111_000.0 * Math.cos(Math.toRadians(lat)));

        BigDecimal latMin = BigDecimal.valueOf(lat - deltaLat);
        BigDecimal latMax = BigDecimal.valueOf(lat + deltaLat);
        BigDecimal lngMin = BigDecimal.valueOf(lng - deltaLng);
        BigDecimal lngMax = BigDecimal.valueOf(lng + deltaLng);

        List<Incidente> grupo = incidenteRepository.buscarSimilaresParaValidacion(
                        incidente.getTipoIncidente(), desde, latMin, latMax, lngMin, lngMax)
                .stream()
                .filter(c -> GeoUtils.distanciaMetros(lat, lng,
                        c.getLatitud().doubleValue(), c.getLongitud().doubleValue()) <= RADIO_METROS)
                .toList();

        // El consenso exige usuarios distintos: una misma persona no puede
        // validar su propio reporte enviándolo varias veces.
        Set<Integer> usuariosDistintos = new HashSet<>();
        for (Incidente c : grupo) {
            if (c.getUsuario() != null) {
                usuariosDistintos.add(c.getUsuario().getIdUsuario());
            }
        }

        if (usuariosDistintos.size() < MIN_REPORTES) {
            return false;
        }

        // Se validan todos los reportes PENDIENTES del grupo, no solo el último.
        String observacion = "Auto-validado: " + grupo.size() + " reportes similares de " +
                usuariosDistintos.size() + " usuarios distintos en " +
                (int) RADIO_METROS + "m / " + VENTANA_HORAS + "h";
        boolean actualValidado = false;
        for (Incidente c : grupo) {
            if (!"PENDIENTE".equals(c.getEstado())) {
                continue;
            }
            validar(c, observacion);
            if (c.getIdIncidente().equals(incidente.getIdIncidente())) {
                actualValidado = true;
            }
        }
        if (!actualValidado) {
            validar(incidente, observacion);
        }
        return true;
    }

    private void validar(Incidente incidente, String observacion) {
        incidente.setEstado("VALIDADO");
        incidenteRepository.save(incidente);

        ValidacionReporte validacion = new ValidacionReporte();
        validacion.setIncidente(incidente);
        validacion.setUsuario(null);
        validacion.setAccion("VALIDADO_AUTO");
        validacion.setObservacion(observacion);
        validacionReporteRepository.save(validacion);

        alertaService.registrarSiCorresponde(incidente);

        eventoLogService.info("VALIDACION_AUTOMATICA",
                "Incidente " + incidente.getIdIncidente() + " auto-validado por consenso de tipo " +
                        incidente.getTipoIncidente());
    }
}

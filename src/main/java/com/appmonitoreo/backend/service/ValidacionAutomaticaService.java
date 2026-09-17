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
import java.util.List;

/**
 * Implementa RF10 / RNF07: valida automáticamente un incidente cuando se
 * acumulan reportes similares (mismo tipo) dentro de un radio de 150 m y
 * una ventana de 24 horas.
 */
@Service
public class ValidacionAutomaticaService {

    static final double RADIO_METROS = 150;
    static final int VENTANA_HORAS = 24;
    static final int MIN_REPORTES = 3;

    private final IncidenteRepository incidenteRepository;
    private final ValidacionReporteRepository validacionReporteRepository;
    private final EventoLogService eventoLogService;

    public ValidacionAutomaticaService(IncidenteRepository incidenteRepository,
                                       ValidacionReporteRepository validacionReporteRepository,
                                       EventoLogService eventoLogService) {
        this.incidenteRepository = incidenteRepository;
        this.validacionReporteRepository = validacionReporteRepository;
        this.eventoLogService = eventoLogService;
    }

    @Transactional
    public boolean evaluarYValidar(Incidente incidente) {
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

        List<Incidente> candidatos = incidenteRepository.buscarSimilaresParaValidacion(
                incidente.getTipoIncidente(), desde, latMin, latMax, lngMin, lngMax);

        long similares = candidatos.stream()
                .filter(c -> GeoUtils.distanciaMetros(lat, lng,
                        c.getLatitud().doubleValue(), c.getLongitud().doubleValue()) <= RADIO_METROS)
                .count();

        if (similares < MIN_REPORTES) {
            return false;
        }

        incidente.setEstado("VALIDADO");
        incidenteRepository.save(incidente);

        ValidacionReporte validacion = new ValidacionReporte();
        validacion.setIncidente(incidente);
        validacion.setUsuario(null);
        validacion.setAccion("VALIDADO_AUTO");
        validacion.setObservacion("Auto-validado: " + similares + " reportes similares en " +
                (int) RADIO_METROS + "m / " + VENTANA_HORAS + "h");
        validacionReporteRepository.save(validacion);

        eventoLogService.info("VALIDACION_AUTOMATICA",
                "Incidente " + incidente.getIdIncidente() + " auto-validado con " + similares +
                        " reportes similares de tipo " + incidente.getTipoIncidente());

        return true;
    }
}

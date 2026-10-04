package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.service.EventoLogService;
import com.appmonitoreo.backend.service.NotificacionService;
import com.appmonitoreo.backend.util.GeoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * RF04: detecta si el usuario se encuentra dentro de una zona de riesgo ALTO
 * y dispara la alerta correspondiente (evento registrado + intento de push).
 */
@RestController
@RequestMapping("/alertas")
public class AlertaController {

    private final IncidenteRepository incidenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final EventoLogService eventoLogService;

    public AlertaController(IncidenteRepository incidenteRepository,
                            UsuarioRepository usuarioRepository,
                            NotificacionService notificacionService,
                            EventoLogService eventoLogService) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.eventoLogService = eventoLogService;
    }

    @GetMapping("/verificar")
    public ResponseEntity<?> verificarUbicacion(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "300") double radioMetros,
            Authentication auth) {

        double delta = radioMetros / 111_000.0;
        BigDecimal latMin = BigDecimal.valueOf(lat - delta);
        BigDecimal latMax = BigDecimal.valueOf(lat + delta);
        BigDecimal lngMin = BigDecimal.valueOf(lng - delta);
        BigDecimal lngMax = BigDecimal.valueOf(lng + delta);

        List<Incidente> cercanosAlto = incidenteRepository.buscarEnCaja(latMin, latMax, lngMin, lngMax)
                .stream()
                .filter(i -> "ALTO".equals(i.getNivelRiesgo()))
                .filter(i -> GeoUtils.distanciaMetros(lat, lng,
                        i.getLatitud().doubleValue(), i.getLongitud().doubleValue()) <= radioMetros)
                .toList();

        boolean enZonaDeRiesgo = !cercanosAlto.isEmpty();

        if (enZonaDeRiesgo) {
            usuarioRepository.findByCorreo(auth.getName()).ifPresent(usuario -> {
                notificacionService.enviarAlertaZonaRiesgo(usuario, cercanosAlto.size());
                eventoLogService.info("ALERTA_PROXIMIDAD",
                        "Usuario " + usuario.getCorreo() + " ingresó a zona de riesgo ALTO (" +
                                cercanosAlto.size() + " incidentes cercanos)", usuario);
            });
        }

        return ResponseEntity.ok(Map.of(
                "enZonaDeRiesgo", enZonaDeRiesgo,
                "incidentesCercanos", cercanosAlto.size(),
                "detalle", cercanosAlto
        ));
    }
}

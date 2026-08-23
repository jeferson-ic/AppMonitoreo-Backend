package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.dto.IncidenteRequest;
import com.appmonitoreo.backend.model.Alerta;
import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.AlertaRepository;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    private static final Map<String, String> RIESGO_POR_TIPO = Map.of(
            "Robo a mano armada", "ALTO",
            "Asalto", "ALTO",
            "Secuestro", "ALTO",
            "Robo", "MEDIO",
            "Vandalismo", "MEDIO",
            "Acoso", "MEDIO",
            "Sospechoso", "BAJO",
            "Accidente", "BAJO"
    );

    private final IncidenteRepository incidenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaRepository alertaRepository;

    public IncidenteController(IncidenteRepository incidenteRepository,
                               UsuarioRepository usuarioRepository,
                               AlertaRepository alertaRepository) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.alertaRepository = alertaRepository;
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody IncidenteRequest req,
                                   Authentication auth) {
        String correo = auth.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Incidente inc = new Incidente();
        inc.setUsuario(usuario);
        inc.setTipoIncidente(req.getTipoIncidente());
        inc.setDescripcion(req.getDescripcion());
        inc.setLatitud(req.getLatitud());
        inc.setLongitud(req.getLongitud());
        String nivelRiesgo = RIESGO_POR_TIPO.getOrDefault(req.getTipoIncidente(), "MEDIO");
        inc.setNivelRiesgo(nivelRiesgo);

        Incidente guardado = incidenteRepository.save(inc);

        if ("ALTO".equals(nivelRiesgo)) {
            Alerta alerta = new Alerta();
            alerta.setIncidente(guardado);
            alerta.setNivelRiesgo("ALTO");
            alerta.setMensaje("Zona de riesgo ALTO: " + req.getTipoIncidente() + " reportado cerca");
            alertaRepository.save(alerta);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping
    public List<Incidente> listar() {
        return incidenteRepository.findByEstadoNot("ELIMINADO");
    }

    @GetMapping("/cercanos")
    public List<Incidente> cercanos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1.0") double radioKm) {
        double delta = radioKm / 111.0;
        BigDecimal latMin = BigDecimal.valueOf(lat - delta);
        BigDecimal latMax = BigDecimal.valueOf(lat + delta);
        BigDecimal lngMin = BigDecimal.valueOf(lng - delta);
        BigDecimal lngMax = BigDecimal.valueOf(lng + delta);
        return incidenteRepository.buscarEnCaja(latMin, latMax, lngMin, lngMax);
    }

    @GetMapping("/mis-reportes")
    public List<Incidente> misReportes(Authentication auth) {
        String correo = auth.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return incidenteRepository.findByUsuarioIdUsuarioAndEstadoNot(
                usuario.getIdUsuario(), "ELIMINADO");
    }

    @GetMapping("/tipos")
    public ResponseEntity<?> tipos() {
        return ResponseEntity.ok(RIESGO_POR_TIPO.keySet().stream().sorted().toList());
    }

    @GetMapping("/zonas-riesgo")
    public ResponseEntity<?> zonasRiesgo() {
        var zonas = incidenteRepository.findByEstadoNot("ELIMINADO").stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> Map.of(
                                "celda_lat", i.getLatitud().setScale(3, java.math.RoundingMode.HALF_UP),
                                "celda_lng", i.getLongitud().setScale(3, java.math.RoundingMode.HALF_UP)
                        ),
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>(e.getKey());
                    m.put("cantidad", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("cantidad"), (Long) a.get("cantidad")))
                .limit(20)
                .toList();
        return ResponseEntity.ok(zonas);
    }
}

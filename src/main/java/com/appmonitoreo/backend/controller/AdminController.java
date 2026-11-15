package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.model.EventoSistema;
import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.model.ValidacionReporte;
import com.appmonitoreo.backend.repository.AlertaRepository;
import com.appmonitoreo.backend.repository.EventoSistemaRepository;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.repository.ValidacionReporteRepository;
import com.appmonitoreo.backend.service.AlertaService;
import com.appmonitoreo.backend.service.EventoLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * Cambios de estado permitidos para el administrador. ELIMINADO es final.
     * Un reporte rechazado puede volver a validarse y uno validado puede
     * rechazarse si se descubre que era falso.
     */
    static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "PENDIENTE", Set.of("VALIDADO", "RECHAZADO", "ELIMINADO"),
            "VALIDADO", Set.of("RECHAZADO", "ELIMINADO"),
            "RECHAZADO", Set.of("VALIDADO", "ELIMINADO")
    );

    private final IncidenteRepository incidenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidacionReporteRepository validacionRepo;
    private final AlertaRepository alertaRepository;
    private final EventoSistemaRepository eventoSistemaRepository;
    private final EventoLogService eventoLogService;
    private final AlertaService alertaService;

    public AdminController(IncidenteRepository incidenteRepository,
                           UsuarioRepository usuarioRepository,
                           ValidacionReporteRepository validacionRepo,
                           AlertaRepository alertaRepository,
                           EventoSistemaRepository eventoSistemaRepository,
                           EventoLogService eventoLogService,
                           AlertaService alertaService) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.validacionRepo = validacionRepo;
        this.alertaRepository = alertaRepository;
        this.eventoSistemaRepository = eventoSistemaRepository;
        this.eventoLogService = eventoLogService;
        this.alertaService = alertaService;
    }

    @GetMapping("/incidentes/pendientes")
    public List<Incidente> pendientes() {
        return incidenteRepository.findByEstado("PENDIENTE");
    }

    @PutMapping("/incidentes/{id}/validar")
    public ResponseEntity<?> validar(@PathVariable Integer id, Authentication auth) {
        return cambiarEstado(id, "VALIDADO", auth);
    }

    @PutMapping("/incidentes/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Integer id, Authentication auth) {
        return cambiarEstado(id, "RECHAZADO", auth);
    }

    @PutMapping("/incidentes/{id}/eliminar")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, Authentication auth) {
        return cambiarEstado(id, "ELIMINADO", auth);
    }

    @GetMapping("/metricas")
    public ResponseEntity<?> metricas() {
        long total = incidenteRepository.count();
        long pendientes = incidenteRepository.countByEstado("PENDIENTE");
        long validados = incidenteRepository.countByEstado("VALIDADO");
        long rechazados = incidenteRepository.countByEstado("RECHAZADO");
        long eliminados = incidenteRepository.countByEstado("ELIMINADO");

        long validacionesAuto = validacionRepo.countByAccion("VALIDADO_AUTO");
        long validacionesManuales = validacionRepo.countByAccion("VALIDADO");
        long totalValidaciones = validacionesAuto + validacionesManuales;
        double porcentajeAuto = totalValidaciones == 0 ? 0
                : (validacionesAuto * 100.0) / totalValidaciones;

        var zonasMasRiesgosas = incidenteRepository.findByEstado("VALIDADO").stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        i -> Map.of(
                                "celda_lat", i.getLatitud().setScale(3, RoundingMode.HALF_UP),
                                "celda_lng", i.getLongitud().setScale(3, RoundingMode.HALF_UP)
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
                .limit(5)
                .toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalIncidentes", total);
        resp.put("porEstado", Map.of(
                "PENDIENTE", pendientes,
                "VALIDADO", validados,
                "RECHAZADO", rechazados,
                "ELIMINADO", eliminados));
        resp.put("validacionesAutomaticas", validacionesAuto);
        resp.put("validacionesManuales", validacionesManuales);
        resp.put("porcentajeValidacionAutomatica", Math.round(porcentajeAuto * 100.0) / 100.0);
        resp.put("totalUsuarios", usuarioRepository.count());
        resp.put("usuariosActivos", usuarioRepository.countByEstado("ACTIVO"));
        resp.put("totalAlertasRiesgoAlto", alertaRepository.count());
        resp.put("zonasMasRiesgosas", zonasMasRiesgosas);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/eventos")
    public ResponseEntity<?> eventos(
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "100") int limite) {
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limite, 1), 500));
        List<EventoSistema> lista = (tipo == null || tipo.isBlank())
                ? eventoSistemaRepository.findAllByOrderByFechaEventoDesc(pageable)
                : eventoSistemaRepository.findByTipoOrderByFechaEventoDesc(tipo.toUpperCase(), pageable);
        return ResponseEntity.ok(lista);
    }

    private ResponseEntity<?> cambiarEstado(Integer id, String nuevoEstado, Authentication auth) {
        return incidenteRepository.findById(id)
                .<ResponseEntity<?>>map(inc -> {
                    String estadoActual = inc.getEstado();
                    if (estadoActual == null
                            || !TRANSICIONES.getOrDefault(estadoActual, Set.of()).contains(nuevoEstado)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                                "No se puede pasar el incidente " + id + " de " + estadoActual +
                                        " a " + nuevoEstado));
                    }

                    inc.setEstado(nuevoEstado);
                    incidenteRepository.save(inc);
                    alertaService.registrarSiCorresponde(inc);

                    String correo = auth.getName();
                    usuarioRepository.findByCorreo(correo).ifPresent(admin -> {
                        ValidacionReporte v = new ValidacionReporte();
                        v.setIncidente(inc);
                        v.setUsuario(admin);
                        v.setAccion(nuevoEstado);
                        validacionRepo.save(v);

                        eventoLogService.info("ADMIN_VALIDACION",
                                "Admin " + admin.getCorreo() + " marcó el incidente " + id +
                                        " como " + nuevoEstado,
                                admin);
                    });

                    return ResponseEntity.ok(Map.of("mensaje",
                            "Incidente " + id + " marcado como " + nuevoEstado));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

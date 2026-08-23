package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.model.ValidacionReporte;
import com.appmonitoreo.backend.repository.IncidenteRepository;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.repository.ValidacionReporteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final IncidenteRepository incidenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidacionReporteRepository validacionRepo;

    public AdminController(IncidenteRepository incidenteRepository,
                           UsuarioRepository usuarioRepository,
                           ValidacionReporteRepository validacionRepo) {
        this.incidenteRepository = incidenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.validacionRepo = validacionRepo;
    }

    @GetMapping("/incidentes/pendientes")
    public List<Incidente> pendientes() {
        return incidenteRepository.findAll().stream()
                .filter(i -> "PENDIENTE".equals(i.getEstado()))
                .toList();
    }

    @PutMapping("/incidentes/{id}/validar")
    public ResponseEntity<?> validar(@PathVariable Integer id, Authentication auth) {
        return cambiarEstado(id, "VALIDADO", auth);
    }

    @PutMapping("/incidentes/{id}/eliminar")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, Authentication auth) {
        return cambiarEstado(id, "ELIMINADO", auth);
    }

    private ResponseEntity<?> cambiarEstado(Integer id, String nuevoEstado, Authentication auth) {
        return incidenteRepository.findById(id)
                .map(inc -> {
                    inc.setEstado(nuevoEstado);
                    incidenteRepository.save(inc);

                    String correo = auth.getName();
                    usuarioRepository.findByCorreo(correo).ifPresent(admin -> {
                        ValidacionReporte v = new ValidacionReporte();
                        v.setIncidente(inc);
                        v.setUsuario(admin);
                        v.setAccion(nuevoEstado);
                        validacionRepo.save(v);
                    });

                    return ResponseEntity.ok(Map.of("mensaje",
                            "Incidente " + id + " marcado como " + nuevoEstado));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

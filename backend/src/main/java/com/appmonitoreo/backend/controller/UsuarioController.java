package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PutMapping("/token-fcm")
    public ResponseEntity<?> actualizarTokenFcm(@RequestBody Map<String, String> body,
                                                 Authentication auth) {
        String tokenFcm = body.get("tokenFcm");
        if (tokenFcm == null || tokenFcm.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "tokenFcm requerido"));
        }
        String correo = auth.getName();
        return usuarioRepository.findByCorreo(correo)
                .map(u -> {
                    u.setTokenFcm(tokenFcm);
                    usuarioRepository.save(u);
                    return ResponseEntity.ok(Map.of("mensaje", "Token FCM actualizado"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

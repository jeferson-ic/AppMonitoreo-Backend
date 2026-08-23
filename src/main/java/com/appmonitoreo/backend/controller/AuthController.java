package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.dto.AuthResponse;
import com.appmonitoreo.backend.dto.LoginRequest;
import com.appmonitoreo.backend.dto.RegisterRequest;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setCorreo(req.getCorreo());
        usuario.setContrasena(passwordEncoder.encode(req.getContrasena()));
        usuario.setTelefono(req.getTelefono());

        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado correctamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return usuarioRepository.findByCorreo(req.getCorreo())
                .filter(u -> "ACTIVO".equals(u.getEstado()))
                .filter(u -> passwordEncoder.matches(req.getContrasena(), u.getContrasena()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getCorreo(), u.getRol());
                    return ResponseEntity.ok((Object) new AuthResponse(token, u.getCorreo(), u.getNombre(), u.getRol()));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales incorrectas")));
    }
}

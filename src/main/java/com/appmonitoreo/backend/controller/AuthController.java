package com.appmonitoreo.backend.controller;

import com.appmonitoreo.backend.dto.AuthResponse;
import com.appmonitoreo.backend.dto.LoginRequest;
import com.appmonitoreo.backend.dto.RegisterRequest;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.UsuarioRepository;
import com.appmonitoreo.backend.security.JwtUtil;
import com.appmonitoreo.backend.security.LoginAttemptService;
import com.appmonitoreo.backend.service.EventoLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EventoLogService eventoLogService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          EventoLogService eventoLogService,
                          LoginAttemptService loginAttemptService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.eventoLogService = eventoLogService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            eventoLogService.warning("AUTH", "Intento de registro con correo ya existente: " + req.getCorreo());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setCorreo(req.getCorreo());
        usuario.setContrasena(passwordEncoder.encode(req.getContrasena()));
        usuario.setTelefono(req.getTelefono());

        usuarioRepository.save(usuario);

        eventoLogService.info("AUTH", "Usuario registrado: " + req.getCorreo(), usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario registrado correctamente"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (loginAttemptService.estaBloqueado(ip)) {
            eventoLogService.warning("AUTH", "Login bloqueado por exceso de intentos desde " + ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Demasiados intentos fallidos. Intente más tarde"));
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(req.getCorreo())
                .filter(u -> "ACTIVO".equals(u.getEstado()))
                .filter(u -> passwordEncoder.matches(req.getContrasena(), u.getContrasena()));

        if (usuarioOpt.isEmpty()) {
            loginAttemptService.registrarFallo(ip);
            eventoLogService.warning("AUTH", "Intento de login fallido para: " + req.getCorreo());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }

        loginAttemptService.registrarExito(ip);
        Usuario u = usuarioOpt.get();
        String token = jwtUtil.generateToken(u.getCorreo(), u.getRol());
        eventoLogService.info("AUTH", "Login exitoso: " + u.getCorreo(), u);
        return ResponseEntity.ok(new AuthResponse(token, u.getCorreo(), u.getNombre(), u.getRol()));
    }
}

package com.appmonitoreo.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloquea temporalmente una IP después de varios intentos de login fallidos
 * para mitigar ataques de fuerza bruta.
 */
@Service
public class LoginAttemptService {

    private record Registro(int fallos, Instant inicio, Instant bloqueadoHasta) {}

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maxIntentos;
    private final Duration ventana;

    public LoginAttemptService(@Value("${app.login.max-intentos:5}") int maxIntentos,
                               @Value("${app.login.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofMinutes(bloqueoMinutos);
    }

    public boolean estaBloqueado(String clave) {
        Registro r = registros.get(clave);
        if (r == null || r.bloqueadoHasta() == null) {
            return false;
        }
        if (Instant.now().isAfter(r.bloqueadoHasta())) {
            registros.remove(clave);
            return false;
        }
        return true;
    }

    public void registrarFallo(String clave) {
        Instant ahora = Instant.now();
        registros.compute(clave, (k, r) -> {
            if (r == null || ahora.isAfter(r.inicio().plus(ventana))) {
                r = new Registro(0, ahora, null);
            }
            int fallos = r.fallos() + 1;
            Instant bloqueo = fallos >= maxIntentos ? ahora.plus(ventana) : null;
            return new Registro(fallos, r.inicio(), bloqueo);
        });
    }

    public void registrarExito(String clave) {
        registros.remove(clave);
    }
}

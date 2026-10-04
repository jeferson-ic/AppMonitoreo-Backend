package com.appmonitoreo.backend.security;

import com.appmonitoreo.backend.service.EventoLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * RNF01: "El tiempo de respuesta del sistema no debe superar los 3 segundos
 * en condiciones normales." Mide cada request y registra un evento WARNING
 * (RF07) cuando se excede el umbral, para poder confirmarlo revisando
 * GET /admin/eventos en vez de instrumentación externa.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TiempoRespuestaFilter extends OncePerRequestFilter {

    private static final long UMBRAL_MS = 3000;

    private final EventoLogService eventoLogService;

    public TiempoRespuestaFilter(EventoLogService eventoLogService) {
        this.eventoLogService = eventoLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duracionMs = System.currentTimeMillis() - inicio;
            if (duracionMs > UMBRAL_MS) {
                eventoLogService.warning("RNF01_TIEMPO_RESPUESTA",
                        request.getMethod() + " " + request.getRequestURI() + " tardó " + duracionMs +
                                " ms (umbral RNF01: " + UMBRAL_MS + " ms)");
            }
        }
    }
}

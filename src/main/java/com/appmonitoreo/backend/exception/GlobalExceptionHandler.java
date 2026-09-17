package com.appmonitoreo.backend.exception;

import com.appmonitoreo.backend.service.EventoLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final EventoLogService eventoLogService;

    public GlobalExceptionHandler(EventoLogService eventoLogService) {
        this.eventoLogService = eventoLogService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> detalles.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos", "detalles", detalles));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> manejarError(Exception ex, HttpServletRequest request) {
        eventoLogService.error("SISTEMA",
                "Error no controlado en " + request.getMethod() + " " + request.getRequestURI() + ": " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor"));
    }
}

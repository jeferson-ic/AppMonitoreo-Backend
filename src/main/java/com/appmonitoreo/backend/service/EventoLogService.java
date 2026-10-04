package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.EventoSistema;
import com.appmonitoreo.backend.model.Usuario;
import com.appmonitoreo.backend.repository.EventoSistemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventoLogService {

    private static final Logger log = LoggerFactory.getLogger(EventoLogService.class);

    private final EventoSistemaRepository eventoSistemaRepository;

    public EventoLogService(EventoSistemaRepository eventoSistemaRepository) {
        this.eventoSistemaRepository = eventoSistemaRepository;
    }

    public void info(String componente, String mensaje) {
        registrar("INFO", componente, mensaje, null);
    }

    public void info(String componente, String mensaje, Usuario usuario) {
        registrar("INFO", componente, mensaje, usuario);
    }

    public void warning(String componente, String mensaje) {
        registrar("WARNING", componente, mensaje, null);
    }

    public void warning(String componente, String mensaje, Usuario usuario) {
        registrar("WARNING", componente, mensaje, usuario);
    }

    public void error(String componente, String mensaje) {
        registrar("ERROR", componente, mensaje, null);
    }

    public void error(String componente, String mensaje, Usuario usuario) {
        registrar("ERROR", componente, mensaje, usuario);
    }

    private void registrar(String tipo, String componente, String mensaje, Usuario usuario) {
        try {
            EventoSistema evento = new EventoSistema();
            evento.setTipo(tipo);
            evento.setComponente(componente);
            evento.setMensaje(mensaje);
            evento.setUsuario(usuario);
            eventoSistemaRepository.save(evento);
        } catch (Exception ex) {
            log.warn("No se pudo persistir el evento de sistema [{}] {}: {}", componente, mensaje, ex.getMessage());
        }

        switch (tipo) {
            case "ERROR" -> log.error("[{}] {}", componente, mensaje);
            case "WARNING" -> log.warn("[{}] {}", componente, mensaje);
            default -> log.info("[{}] {}", componente, mensaje);
        }
    }
}

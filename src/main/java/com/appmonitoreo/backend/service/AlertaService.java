package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.Alerta;
import com.appmonitoreo.backend.model.Incidente;
import com.appmonitoreo.backend.repository.AlertaRepository;
import org.springframework.stereotype.Service;

/**
 * Registra la alerta de un incidente de riesgo ALTO solo cuando el incidente
 * ya fue VALIDADO (por consenso o por el administrador). Evita alertas
 * duplicadas para un mismo incidente.
 */
@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public boolean registrarSiCorresponde(Incidente incidente) {
        if (!"VALIDADO".equals(incidente.getEstado())
                || !"ALTO".equals(incidente.getNivelRiesgo())
                || alertaRepository.existsByIncidenteIdIncidente(incidente.getIdIncidente())) {
            return false;
        }
        Alerta alerta = new Alerta();
        alerta.setIncidente(incidente);
        alerta.setNivelRiesgo("ALTO");
        alerta.setMensaje("Zona de riesgo ALTO: " + incidente.getTipoIncidente() + " reportado cerca");
        alertaRepository.save(alerta);
        return true;
    }
}

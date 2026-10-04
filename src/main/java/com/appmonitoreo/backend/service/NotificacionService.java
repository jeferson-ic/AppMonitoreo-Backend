package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.Usuario;

public interface NotificacionService {
    void enviarAlertaZonaRiesgo(Usuario usuario, int incidentesCercanos);
}

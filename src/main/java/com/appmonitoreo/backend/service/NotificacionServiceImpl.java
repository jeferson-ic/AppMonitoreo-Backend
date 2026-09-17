package com.appmonitoreo.backend.service;

import com.appmonitoreo.backend.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementación de RF04 (alerta al ingresar a una zona de riesgo).
 *
 * La detección de proximidad y el registro del evento ya son reales
 * (ver AlertaController /alertas/verificar). El envío de la notificación
 * push por Firebase Cloud Messaging queda como punto de extensión: este
 * repositorio no incluye credenciales de servicio de Firebase, así que
 * aquí solo se deja constancia (log) de que la notificación debería
 * enviarse. Para activar el push real:
 *   1. Agregar la dependencia "com.google.firebase:firebase-admin" al pom.xml.
 *   2. Colocar el archivo de credenciales de la cuenta de servicio de Firebase.
 *   3. Inicializar FirebaseApp y reemplazar el cuerpo de este método por una
 *      llamada a FirebaseMessaging.getInstance().send(...) usando
 *      usuario.getTokenFcm().
 *
 * Mientras tanto, la app cliente puede lograr el mismo efecto funcional
 * haciendo polling periódico a /alertas/verificar con la ubicación actual.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    @Override
    public void enviarAlertaZonaRiesgo(Usuario usuario, int incidentesCercanos) {
        if (usuario.getTokenFcm() == null || usuario.getTokenFcm().isBlank()) {
            log.warn("No se puede enviar push a {}: no tiene tokenFcm registrado", usuario.getCorreo());
            return;
        }
        log.info("[PUSH PENDIENTE DE INTEGRACION FCM] Alerta de zona de riesgo para {} ({} incidentes cercanos)",
                usuario.getCorreo(), incidentesCercanos);
    }
}

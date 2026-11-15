package com.appmonitoreo.backend.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Hora oficial de la aplicación. El servidor de Render trabaja en UTC, por lo
 * que las fechas se generan siempre en hora de Perú (UTC-5) para que la app
 * muestre la hora correcta al usuario.
 */
public final class FechaUtil {

    public static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private FechaUtil() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA_PERU);
    }
}

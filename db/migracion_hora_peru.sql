-- =====================================================================
-- Migración única: fechas de UTC a hora de Perú (UTC-5)
-- ---------------------------------------------------------------------
-- Hasta esta versión, el backend guardaba las fechas en UTC (hora del
-- servidor de Render) y la app las mostraba con 5 horas de adelanto.
-- Desde esta versión las fechas se generan en hora de Perú (FechaUtil),
-- así que los registros antiguos se corrigen restando 5 horas.
--
-- IMPORTANTE
-- * Ejecutar UNA SOLA VEZ. Si se ejecuta dos veces, las fechas quedarán
--   10 horas atrasadas.
-- * Ejecutar justo cuando la nueva versión del backend ya esté desplegada
--   en Render, sin usuarios creando reportes en ese momento.
-- * Antes, sacar un respaldo desde la consola de Aiven.
-- * Si después se vuelve a ejecutar db/datos_prueba_aiven.sql, NO hace
--   falta repetir esta migración: ese script ya usa la hora de Perú.
-- =====================================================================

-- Debe ser la MISMA base que aparece en DB_URL de Render (ej. defaultdb)
USE defaultdb;

SET @safe_updates_previo = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

UPDATE usuarios            SET fecha_registro  = fecha_registro  - INTERVAL 5 HOUR WHERE fecha_registro  IS NOT NULL;
UPDATE incidentes          SET fecha_incidente = fecha_incidente - INTERVAL 5 HOUR WHERE fecha_incidente IS NOT NULL;
UPDATE alertas             SET fecha_alerta    = fecha_alerta    - INTERVAL 5 HOUR WHERE fecha_alerta    IS NOT NULL;
UPDATE validacion_reportes SET fecha_accion    = fecha_accion    - INTERVAL 5 HOUR WHERE fecha_accion    IS NOT NULL;
UPDATE eventos_sistema     SET fecha_evento    = fecha_evento    - INTERVAL 5 HOUR WHERE fecha_evento    IS NOT NULL;

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_previo;

-- Verificación: el último incidente debe mostrar una hora cercana a la hora
-- actual de Lima.
SELECT id_incidente, tipo_incidente, fecha_incidente
FROM incidentes
ORDER BY fecha_incidente DESC
LIMIT 5;

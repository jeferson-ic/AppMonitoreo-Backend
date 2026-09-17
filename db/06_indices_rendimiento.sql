-- Script 06: índices de rendimiento para RF09 (filtros) y RF10/RNF07 (validación automática < 5s)
-- Ejecutar una sola vez.
USE db_zonas_peligrosas;

ALTER TABLE incidentes ADD INDEX idx_tipo_fecha (tipo_incidente, fecha_incidente);
ALTER TABLE incidentes ADD INDEX idx_estado (estado);

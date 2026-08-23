-- Script 03: tabla alertas
USE db_zonas_peligrosas;

CREATE TABLE IF NOT EXISTS alertas (
    id_alerta    INT PRIMARY KEY AUTO_INCREMENT,
    id_incidente INT,
    mensaje      VARCHAR(255),
    nivel_riesgo VARCHAR(20),
    fecha_alerta DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente)
);

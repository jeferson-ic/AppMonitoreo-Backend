-- Script 02: tabla incidentes
USE db_zonas_peligrosas;

CREATE TABLE IF NOT EXISTS incidentes (
    id_incidente    INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario      INT,
    tipo_incidente  VARCHAR(50),
    descripcion     TEXT,
    latitud         DECIMAL(10,7),
    longitud        DECIMAL(10,7),
    nivel_riesgo    VARCHAR(20) DEFAULT 'MEDIO',   -- ALTO | MEDIO | BAJO
    fecha_incidente DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado          VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE | VALIDADO | ELIMINADO
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    INDEX idx_ubicacion (latitud, longitud)
);

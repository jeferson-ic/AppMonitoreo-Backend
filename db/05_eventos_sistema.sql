-- Script 05: tabla eventos_sistema (RF07 - registro interno de eventos)
USE db_zonas_peligrosas;

CREATE TABLE IF NOT EXISTS eventos_sistema (
    id_evento     INT PRIMARY KEY AUTO_INCREMENT,
    tipo          VARCHAR(10) NOT NULL,      -- INFO | WARNING | ERROR
    componente    VARCHAR(50),
    mensaje       TEXT,
    id_usuario    INT,
    fecha_evento  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    INDEX idx_evento_tipo_fecha (tipo, fecha_evento)
);

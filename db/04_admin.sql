USE db_zonas_peligrosas;

CREATE TABLE IF NOT EXISTS validacion_reportes (
    id_validacion INT PRIMARY KEY AUTO_INCREMENT,
    id_incidente  INT,
    id_usuario    INT,
    accion        VARCHAR(20),
    fecha_accion  DATETIME DEFAULT CURRENT_TIMESTAMP,
    observacion   VARCHAR(255),
    FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente),
    FOREIGN KEY (id_usuario)   REFERENCES usuarios(id_usuario)
);

INSERT IGNORE INTO usuarios (nombre, correo, contrasena, rol, estado)
VALUES (
    'Administrador',
    'admin@zonas.com',
    '$2a$10$FP3UO.ETrXtFQPzynEXesepG.FPS/8Me5uRDhEAq0zE.G.FQ4ZlCG',
    'ADMIN',
    'ACTIVO'
);

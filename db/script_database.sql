CREATE DATABASE IF NOT EXISTS db_zonas_peligrosas;

USE db_zonas_peligrosas;

-- Script 01: tabla usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario   INT PRIMARY KEY AUTO_INCREMENT,
    nombre       VARCHAR(100) NOT NULL,
    correo       VARCHAR(100) UNIQUE NOT NULL,
    contrasena   VARCHAR(255) NOT NULL,      -- hash BCrypt, nunca texto plano
    telefono     VARCHAR(15),
    token_fcm    VARCHAR(255),               -- para notificaciones push (HU12)
    rol          VARCHAR(20) DEFAULT 'USUARIO',  -- USUARIO | ADMIN
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado       VARCHAR(20) DEFAULT 'ACTIVO'
);

-- Script 02: tabla incidentes
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

-- Script 03: tabla alertas
CREATE TABLE IF NOT EXISTS alertas (
    id_alerta    INT PRIMARY KEY AUTO_INCREMENT,
    id_incidente INT,
    mensaje      VARCHAR(255),
    nivel_riesgo VARCHAR(20),
    fecha_alerta DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_incidente) REFERENCES incidentes(id_incidente)
);

-- Script 04: tabla validacion_reportes
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

-- Script 05: tabla eventos_sistema (RF07 - registro interno de eventos)
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

-- Script 06: índices de rendimiento para RF09 (filtros) y RF10/RNF07 (validación automática < 5s)
-- Ejecutar una sola vez.
ALTER TABLE incidentes ADD INDEX idx_tipo_fecha (tipo_incidente, fecha_incidente);
ALTER TABLE incidentes ADD INDEX idx_estado (estado);
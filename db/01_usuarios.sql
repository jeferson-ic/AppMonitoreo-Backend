-- Script 01: tabla usuarios
-- Ejecutar después de crear la base de datos db_zonas_peligrosas

USE db_zonas_peligrosas;

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

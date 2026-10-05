-- =====================================================================
-- Datos de prueba para demos / avances  (AlertaZona - Lima, Perú)
-- ---------------------------------------------------------------------
-- Requisito: haber ejecutado antes db/script_database.sql
--
-- * Re-ejecutable: borra primero todo lo creado por este script
--   (usuarios con correo @demo.pe y todo lo que cuelga de ellos).
-- * Las fechas son relativas a NOW(), así que el mapa siempre muestra
--   incidentes "de hoy", "de ayer", "hace 2 semanas", etc.
-- * Contraseña de TODOS los usuarios demo:  Demo1234
--     admin.demo@demo.pe      -> ADMIN (panel admin + métricas)
--     lucia.ramirez@demo.pe   -> USUARIO (tiene varios reportes)
-- * Para probar las alertas de proximidad en el emulador, fija la
--   ubicación en Parque Kennedy, Miraflores:  -12.1219, -77.0297
-- =====================================================================

USE db_zonas_peligrosas;
SET NAMES utf8mb4;
SET @safe_updates_previo = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 0. Limpieza de una ejecución anterior
-- ---------------------------------------------------------------------
DELETE e FROM eventos_sistema e
JOIN incidentes i ON e.mensaje LIKE CONCAT('Incidente ', i.id_incidente, ' %')
                  OR e.mensaje LIKE CONCAT('% el incidente ', i.id_incidente, ' como %')
JOIN usuarios u   ON u.id_usuario = i.id_usuario
WHERE u.correo LIKE '%@demo.pe';

DELETE FROM eventos_sistema
WHERE id_usuario IN (SELECT id_usuario FROM usuarios WHERE correo LIKE '%@demo.pe')
   OR mensaje LIKE '%@demo.pe%';

DELETE FROM validacion_reportes
WHERE id_incidente IN (SELECT i.id_incidente FROM incidentes i
                       JOIN usuarios u ON u.id_usuario = i.id_usuario
                       WHERE u.correo LIKE '%@demo.pe')
   OR id_usuario IN (SELECT id_usuario FROM usuarios WHERE correo LIKE '%@demo.pe');

DELETE FROM alertas
WHERE id_incidente IN (SELECT i.id_incidente FROM incidentes i
                       JOIN usuarios u ON u.id_usuario = i.id_usuario
                       WHERE u.correo LIKE '%@demo.pe');

DELETE FROM incidentes
WHERE id_usuario IN (SELECT id_usuario FROM usuarios WHERE correo LIKE '%@demo.pe');

DELETE FROM usuarios WHERE correo LIKE '%@demo.pe';

-- ---------------------------------------------------------------------
-- 1. Usuarios  (hash BCrypt de "Demo1234")
-- ---------------------------------------------------------------------
SET @ahora := NOW();
SET @hash  := '$2a$10$NCFjF3zTgbXZ7O5RT13xOebnyQeHpIHDvVbyk0g2ijU24.xIIJbjW';

INSERT INTO usuarios (nombre, correo, contrasena, telefono, rol, fecha_registro, estado) VALUES
('Moderador Demo',        'admin.demo@demo.pe',       @hash, '999000111', 'ADMIN',   @ahora - INTERVAL 90 DAY, 'ACTIVO'),
('Lucía Ramírez Torres',  'lucia.ramirez@demo.pe',    @hash, '987654321', 'USUARIO', @ahora - INTERVAL 60 DAY, 'ACTIVO'),
('Carlos Mendoza Quispe', 'carlos.mendoza@demo.pe',   @hash, '912345678', 'USUARIO', @ahora - INTERVAL 55 DAY, 'ACTIVO'),
('Valeria Huamán Rojas',  'valeria.huaman@demo.pe',   @hash, '945612378', 'USUARIO', @ahora - INTERVAL 48 DAY, 'ACTIVO'),
('Diego Salazar Paredes', 'diego.salazar@demo.pe',    @hash, '956789123', 'USUARIO', @ahora - INTERVAL 45 DAY, 'ACTIVO'),
('Camila Flores Vargas',  'camila.flores@demo.pe',    @hash, '998877665', 'USUARIO', @ahora - INTERVAL 40 DAY, 'ACTIVO'),
('Jorge Castillo Ríos',   'jorge.castillo@demo.pe',   @hash, '923456781', 'USUARIO', @ahora - INTERVAL 30 DAY, 'ACTIVO'),
('Andrea Chávez León',    'andrea.chavez@demo.pe',    @hash, '934567812', 'USUARIO', @ahora - INTERVAL 21 DAY, 'ACTIVO'),
('Renato Gutiérrez Silva','renato.gutierrez@demo.pe', @hash, '967812345', 'USUARIO', @ahora - INTERVAL 35 DAY, 'INACTIVO');

SET @admin := (SELECT id_usuario FROM usuarios WHERE correo = 'admin.demo@demo.pe');

-- ---------------------------------------------------------------------
-- 2. Incidentes
--    nivel según RIESGO_POR_TIPO del backend:
--      ALTO : Robo a mano armada, Asalto, Secuestro
--      MEDIO: Robo, Vandalismo, Acoso (y tipos "Otro", p. ej. Incendio)
--      BAJO : Sospechoso, Accidente
--    validacion: AUTO (VALIDADO_AUTO), MANUAL (admin valida),
--                RECHAZO, ELIMINA o NULL (sin acción todavía)
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_inc;
CREATE TEMPORARY TABLE seed_inc (
    correo      VARCHAR(100),
    tipo        VARCHAR(50),
    nivel       VARCHAR(20),
    descripcion TEXT,
    lat         DECIMAL(10,7),
    lng         DECIMAL(10,7),
    horas       INT,
    estado      VARCHAR(20),
    validacion  VARCHAR(10)
);

INSERT INTO seed_inc VALUES
-- Zona caliente 1: Gamarra, La Victoria
('camila.flores@demo.pe',  'Asalto', 'ALTO',  'Dos sujetos en moto asaltaron a una comerciante en la cuadra 6 de Jr. Gamarra.',              -12.0660000, -77.0138000,  20, 'VALIDADO',  'MANUAL'),
('andrea.chavez@demo.pe',  'Asalto', 'ALTO',  'Asaltaron a un cliente al salir de una galería, se llevaron su mochila y celular.',          -12.0662000, -77.0140000,   9, 'PENDIENTE', NULL),
('carlos.mendoza@demo.pe', 'Asalto', 'ALTO',  'Otro asalto en la esquina de Gamarra con Huánuco, mismo modus operandi en moto.',            -12.0659000, -77.0137000,   3, 'VALIDADO',  'AUTO'),
('valeria.huaman@demo.pe', 'Robo',   'MEDIO', 'Robo de celular al paso dentro del emporio, cerca del paradero de Av. Aviación.',            -12.0661000, -77.0142000,  50, 'VALIDADO',  'MANUAL'),
('diego.salazar@demo.pe',  'Robo',   'MEDIO', 'Le abrieron la cartera a una señora en plena aglomeración de compras.',                      -12.0663000, -77.0139000, 130, 'VALIDADO',  'MANUAL'),
('jorge.castillo@demo.pe', 'Robo',   'MEDIO', 'Reporte duplicado de robo de celular en Gamarra.',                                           -12.0660000, -77.0141000, 300, 'RECHAZADO', 'RECHAZO'),

-- Zona caliente 2: Parque Kennedy, Miraflores
('lucia.ramirez@demo.pe',  'Robo',               'MEDIO', 'Robo de celular a un turista frente a la iglesia Virgen Milagrosa.',                      -12.1219000, -77.0299000,  22, 'PENDIENTE', NULL),
('valeria.huaman@demo.pe', 'Robo',               'MEDIO', 'Arrebato de celular en la salida del parque hacia Av. Diagonal.',                         -12.1221000, -77.0301000,  12, 'PENDIENTE', NULL),
('camila.flores@demo.pe',  'Robo',               'MEDIO', 'Tercer robo de celular en la zona del Parque Kennedy en el día.',                          -12.1218000, -77.0297000,   1, 'VALIDADO',  'AUTO'),
('lucia.ramirez@demo.pe',  'Robo a mano armada', 'ALTO',  'Asaltaron un local de comida con arma de fuego a la medianoche.',                         -12.1222000, -77.0298000,  70, 'VALIDADO',  'MANUAL'),
('andrea.chavez@demo.pe',  'Acoso',              'MEDIO', 'Un hombre sigue y acosa verbalmente a mujeres en la calle Berlín.',                        -12.1220000, -77.0300000, 200, 'VALIDADO',  'MANUAL'),

-- Zona caliente 3: Mesa Redonda / Mercado Central, Cercado de Lima
('diego.salazar@demo.pe',  'Robo',               'MEDIO', 'Banda de carteristas actuando entre los puestos de Mesa Redonda.',                         -12.0560000, -77.0270000,   6, 'PENDIENTE', NULL),
('carlos.mendoza@demo.pe', 'Acoso',              'MEDIO', 'Acoso a vendedoras por parte de un grupo de jóvenes en Jr. Andahuaylas.',                  -12.0562000, -77.0268000,  30, 'VALIDADO',  'MANUAL'),
('jorge.castillo@demo.pe', 'Robo a mano armada', 'ALTO',  'Robo con cuchillo a un cambista en la esquina de Jr. Ucayali.',                            -12.0558000, -77.0272000, 160, 'VALIDADO',  'MANUAL'),
('diego.salazar@demo.pe',  'Vandalismo',         'MEDIO', 'Reporte de prueba creado por error.',                                                      -12.0561000, -77.0269000, 400, 'ELIMINADO', 'ELIMINA'),

-- Zona caliente 4: Bayóvar, San Juan de Lurigancho
('jorge.castillo@demo.pe', 'Asalto',             'ALTO',  'Asalto a pasajeros de una combi en el paradero de la Estación Bayóvar.',                   -11.9560000, -76.9930000,   5, 'PENDIENTE', NULL),
('jorge.castillo@demo.pe', 'Robo a mano armada', 'ALTO',  'Delincuentes armados robaron una bodega en la Av. Próceres de la Independencia.',           -11.9562000, -76.9928000,  48, 'VALIDADO',  'MANUAL'),
('valeria.huaman@demo.pe', 'Sospechoso',         'BAJO',  'Vehículo sin placas estacionado varias horas con personas observando la zona.',            -11.9558000, -76.9932000,  90, 'PENDIENTE', NULL),

-- Incidentes dispersos por Lima
('lucia.ramirez@demo.pe',  'Vandalismo',         'MEDIO', 'Pintas y daños en las bancas de la Plaza San Martín tras una marcha.',                     -12.0516000, -77.0347000,  15, 'PENDIENTE', NULL),
('andrea.chavez@demo.pe',  'Accidente',          'BAJO',  'Choque entre un bus y un auto en Av. Abancay, tráfico detenido.',                          -12.0508000, -77.0291000,   2, 'PENDIENTE', NULL),
('camila.flores@demo.pe',  'Sospechoso',         'BAJO',  'Persona merodeando los estacionamientos del centro financiero de San Isidro.',             -12.0912000, -77.0230000,  26, 'PENDIENTE', NULL),
('carlos.mendoza@demo.pe', 'Robo',               'MEDIO', 'Robo de espejos de autos en Av. Arequipa a la altura de Risso.',                           -12.0846000, -77.0339000,  75, 'VALIDADO',  'MANUAL'),
('diego.salazar@demo.pe',  'Accidente',          'BAJO',  'Atropello leve a un motociclista en la entrada del Jockey Plaza.',                         -12.0857000, -76.9772000, 110, 'VALIDADO',  'MANUAL'),
('lucia.ramirez@demo.pe',  'Acoso',              'MEDIO', 'Acoso callejero cerca del Puente de los Suspiros por la noche.',                           -12.1494000, -77.0222000,  36, 'PENDIENTE', NULL),
('valeria.huaman@demo.pe', 'Robo',               'MEDIO', 'Robo de bicicleta estacionada frente a un bar en Barranco.',                               -12.1480000, -77.0210000, 250, 'VALIDADO',  'MANUAL'),
('carlos.mendoza@demo.pe', 'Robo a mano armada', 'ALTO',  'Asalto con arma a estudiantes en Av. Venezuela, frente a la ciudad universitaria.',        -12.0560000, -77.0840000,  18, 'PENDIENTE', NULL),
('jorge.castillo@demo.pe', 'Secuestro',          'ALTO',  'Intento de secuestro al paso a un pasajero de taxi cerca de la Plaza Grau del Callao.',    -12.0566000, -77.1475000,  60, 'PENDIENTE', NULL),
('andrea.chavez@demo.pe',  'Asalto',             'ALTO',  'Asaltaron a una vecina al bajar del Metropolitano en la Estación Naranjal.',              -11.9850000, -77.0590000,   8, 'PENDIENTE', NULL),
('camila.flores@demo.pe',  'Robo',               'MEDIO', 'Robo de celular en el paradero frente a Plaza Norte.',                                     -12.0060000, -77.0590000, 180, 'VALIDADO',  'MANUAL'),
('valeria.huaman@demo.pe', 'Incendio',           'MEDIO', 'Amago de incendio en un poste de alta tensión en Av. Primavera, Surco.',                   -12.1105000, -76.9920000,  40, 'PENDIENTE', NULL),
('camila.flores@demo.pe',  'Sospechoso',         'BAJO',  'Reporte sin información suficiente en el Malecón de Miraflores.',                          -12.1290000, -77.0350000, 700, 'RECHAZADO', 'RECHAZO'),
('diego.salazar@demo.pe',  'Vandalismo',         'MEDIO', 'Rotura de lunas de autos estacionados en Chorrillos.',                                     -12.1690000, -77.0230000, 520, 'VALIDADO',  'MANUAL'),
('lucia.ramirez@demo.pe',  'Robo',               'MEDIO', 'Carterista en el Jr. de la Unión a la altura de la Iglesia de La Merced.',                 -12.0475000, -77.0310000, 900, 'VALIDADO',  'MANUAL'),
('andrea.chavez@demo.pe',  'Robo',               'MEDIO', 'Robo de mercadería a un vendedor en el mercado de Surquillo.',                             -12.1130000, -77.0200000, 1000,'VALIDADO',  'MANUAL'),
('carlos.mendoza@demo.pe', 'Asalto',             'ALTO',  'Asalto a un repartidor en Av. México, La Victoria.',                                       -12.0750000, -77.0160000, 600, 'VALIDADO',  'MANUAL'),
('renato.gutierrez@demo.pe','Accidente',         'BAJO',  'Reporte eliminado: ubicación incorrecta en Breña.',                                        -12.0600000, -77.0500000, 350, 'ELIMINADO', 'ELIMINA'),
('renato.gutierrez@demo.pe','Sospechoso',        'BAJO',  'Reporte rechazado: contenido no relacionado en San Borja.',                                -12.0950000, -77.0000000, 450, 'RECHAZADO', 'RECHAZO');

INSERT INTO incidentes (id_usuario, tipo_incidente, descripcion, latitud, longitud,
                        nivel_riesgo, fecha_incidente, estado)
SELECT u.id_usuario, s.tipo, s.descripcion, s.lat, s.lng, s.nivel,
       @ahora - INTERVAL (s.horas * 60 + MOD(s.horas * 17, 60)) MINUTE,
       s.estado
FROM seed_inc s
JOIN usuarios u ON u.correo = s.correo
ORDER BY s.horas DESC;

-- Vista auxiliar: incidente recién insertado + su fila semilla
DROP TEMPORARY TABLE IF EXISTS seed_map;
CREATE TEMPORARY TABLE seed_map AS
SELECT i.id_incidente, i.tipo_incidente, i.nivel_riesgo, i.estado, i.fecha_incidente,
       u.id_usuario, u.correo, s.validacion
FROM seed_inc s
JOIN usuarios u   ON u.correo = s.correo
JOIN incidentes i ON i.id_usuario = u.id_usuario AND i.descripcion = s.descripcion;

-- ---------------------------------------------------------------------
-- 3. Alertas (el backend crea una por cada incidente de riesgo ALTO)
-- ---------------------------------------------------------------------
INSERT INTO alertas (id_incidente, mensaje, nivel_riesgo, fecha_alerta)
SELECT id_incidente,
       CONCAT('Zona de riesgo ALTO: ', tipo_incidente, ' reportado cerca'),
       'ALTO', fecha_incidente
FROM seed_map
WHERE nivel_riesgo = 'ALTO';

-- ---------------------------------------------------------------------
-- 4. Validaciones (manuales del admin y automáticas)
-- ---------------------------------------------------------------------
INSERT INTO validacion_reportes (id_incidente, id_usuario, accion, fecha_accion, observacion)
SELECT id_incidente,
       CASE validacion WHEN 'AUTO' THEN NULL ELSE @admin END,
       CASE validacion
            WHEN 'AUTO'    THEN 'VALIDADO_AUTO'
            WHEN 'MANUAL'  THEN 'VALIDADO'
            WHEN 'RECHAZO' THEN 'RECHAZADO'
            WHEN 'ELIMINA' THEN 'ELIMINADO'
       END,
       CASE validacion
            WHEN 'AUTO' THEN fecha_incidente
            ELSE LEAST(fecha_incidente + INTERVAL 2 HOUR, @ahora)
       END,
       CASE validacion WHEN 'AUTO' THEN 'Auto-validado: 3 reportes similares en 150m / 24h' END
FROM seed_map
WHERE validacion IS NOT NULL;

-- ---------------------------------------------------------------------
-- 5. Eventos del sistema (RF07)
-- ---------------------------------------------------------------------
-- Registros de usuarios
INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento)
SELECT 'INFO', 'AUTH', CONCAT('Usuario registrado: ', correo), id_usuario, fecha_registro
FROM usuarios WHERE correo LIKE '%@demo.pe';

-- Creación de incidentes
INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento)
SELECT 'INFO', 'INCIDENTE',
       CONCAT('Incidente ', id_incidente, ' creado por ', correo, ' (tipo=', nivel_riesgo,
              IF(validacion = 'AUTO', ', auto-validado', ''), ')'),
       id_usuario, fecha_incidente
FROM seed_map;

-- Validaciones automáticas
INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento)
SELECT 'INFO', 'VALIDACION_AUTOMATICA',
       CONCAT('Incidente ', id_incidente, ' auto-validado con 3 reportes similares de tipo ', tipo_incidente),
       NULL, fecha_incidente
FROM seed_map WHERE validacion = 'AUTO';

-- Acciones del admin
INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento)
SELECT 'INFO', 'ADMIN_VALIDACION',
       CONCAT('Admin admin.demo@demo.pe marcó el incidente ', id_incidente, ' como ', estado),
       @admin, LEAST(fecha_incidente + INTERVAL 2 HOUR, @ahora)
FROM seed_map WHERE validacion IN ('MANUAL', 'RECHAZO', 'ELIMINA');

-- Alertas de proximidad, logins fallidos y un aviso de rendimiento
INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento)
SELECT 'INFO', 'ALERTA_PROXIMIDAD',
       'Usuario lucia.ramirez@demo.pe ingresó a zona de riesgo ALTO (1 incidentes cercanos)',
       id_usuario, @ahora - INTERVAL 50 MINUTE
FROM usuarios WHERE correo = 'lucia.ramirez@demo.pe'
UNION ALL
SELECT 'INFO', 'ALERTA_PROXIMIDAD',
       'Usuario jorge.castillo@demo.pe ingresó a zona de riesgo ALTO (2 incidentes cercanos)',
       id_usuario, @ahora - INTERVAL 4 HOUR
FROM usuarios WHERE correo = 'jorge.castillo@demo.pe';

INSERT INTO eventos_sistema (tipo, componente, mensaje, id_usuario, fecha_evento) VALUES
('WARNING', 'AUTH', 'Intento de login fallido para: carlos.mendoza@demo.pe', NULL, @ahora - INTERVAL 7 HOUR),
('WARNING', 'AUTH', 'Intento de login fallido para: desconocido@demo.pe',    NULL, @ahora - INTERVAL 31 HOUR),
('WARNING', 'AUTH', 'Login bloqueado por exceso de intentos desde 192.168.1.45 (@demo.pe)', NULL, @ahora - INTERVAL 31 HOUR),
('WARNING', 'RNF07_VALIDACION_AUTOMATICA',
            'La evaluación tardó 5230 ms (umbral RNF07: 5000 ms) [demo @demo.pe]',  NULL, @ahora - INTERVAL 3 DAY);

DROP TEMPORARY TABLE IF EXISTS seed_map;
DROP TEMPORARY TABLE IF EXISTS seed_inc;

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_previo;

-- ---------------------------------------------------------------------
-- 6. Verificación rápida
-- ---------------------------------------------------------------------
SELECT estado, COUNT(*) AS cantidad
FROM incidentes GROUP BY estado;

SELECT nivel_riesgo, COUNT(*) AS cantidad
FROM incidentes WHERE estado NOT IN ('ELIMINADO', 'RECHAZADO') GROUP BY nivel_riesgo;

SELECT accion, COUNT(*) AS cantidad
FROM validacion_reportes GROUP BY accion;

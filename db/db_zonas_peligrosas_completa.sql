-- MySQL dump 10.13  Distrib 9.4.0, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: db_zonas_peligrosas
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `db_zonas_peligrosas`
--

/*!40000 DROP DATABASE IF EXISTS `db_zonas_peligrosas`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `db_zonas_peligrosas` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `db_zonas_peligrosas`;

--
-- Table structure for table `alertas`
--

DROP TABLE IF EXISTS `alertas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alertas` (
  `id_alerta` int NOT NULL AUTO_INCREMENT,
  `id_incidente` int DEFAULT NULL,
  `mensaje` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nivel_riesgo` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_alerta` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_alerta`),
  KEY `id_incidente` (`id_incidente`),
  CONSTRAINT `alertas_ibfk_1` FOREIGN KEY (`id_incidente`) REFERENCES `incidentes` (`id_incidente`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alertas`
--

LOCK TABLES `alertas` WRITE;
/*!40000 ALTER TABLE `alertas` DISABLE KEYS */;
INSERT INTO `alertas` VALUES (1,2,'Zona de riesgo ALTO: Asalto reportado cerca','ALTO','2026-06-06 22:59:14'),(2,4,'Zona de riesgo ALTO: Asalto reportado cerca','ALTO','2026-06-07 06:47:08'),(3,6,'Zona de riesgo ALTO: Asalto reportado cerca','ALTO','2026-06-07 06:49:45'),(4,9,'Zona de riesgo ALTO: Asalto reportado cerca','ALTO','2026-06-07 07:05:10'),(5,10,'Zona de riesgo ALTO: Robo a mano armada reportado cerca','ALTO','2026-06-07 18:51:30'),(6,11,'Zona de riesgo ALTO: Robo a mano armada reportado cerca','ALTO','2026-06-07 19:10:28'),(7,12,'Zona de riesgo ALTO: Asalto reportado cerca','ALTO','2026-06-07 19:10:30'),(8,13,'Zona de riesgo ALTO: Secuestro reportado cerca','ALTO','2026-06-07 19:10:32');
/*!40000 ALTER TABLE `alertas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `incidentes`
--

DROP TABLE IF EXISTS `incidentes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incidentes` (
  `id_incidente` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `tipo_incidente` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci,
  `latitud` decimal(10,7) DEFAULT NULL,
  `longitud` decimal(10,7) DEFAULT NULL,
  `nivel_riesgo` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MEDIO',
  `fecha_incidente` datetime DEFAULT CURRENT_TIMESTAMP,
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id_incidente`),
  KEY `id_usuario` (`id_usuario`),
  KEY `idx_ubicacion` (`latitud`,`longitud`),
  CONSTRAINT `incidentes_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `incidentes`
--

LOCK TABLES `incidentes` WRITE;
/*!40000 ALTER TABLE `incidentes` DISABLE KEYS */;
INSERT INTO `incidentes` VALUES (1,2,'Robo a mano armada','Asalto en esquina',-12.0464000,-77.0428000,'ALTO','2026-06-06 22:56:19','VALIDADO'),(2,2,'Asalto','Asalto con arma',-12.0500000,-77.0500000,'ALTO','2026-06-06 22:59:14','ELIMINADO'),(3,2,'Accidente','test',37.4219983,-122.0840000,'BAJO','2026-06-07 06:36:42','VALIDADO'),(4,6,'Asalto','QA alto 20260607014706',37.4219999,-122.0840575,'ALTO','2026-06-07 06:47:08','VALIDADO'),(5,6,'Accidente','QA eliminar 20260607014706',37.4229999,-122.0830575,'BAJO','2026-06-07 06:47:08','ELIMINADO'),(6,7,'Asalto','QA alto 20260607014943',37.4219999,-122.0840575,'ALTO','2026-06-07 06:49:45','VALIDADO'),(7,7,'Accidente','QA eliminar 20260607014943',37.4229999,-122.0830575,'BAJO','2026-06-07 06:49:45','ELIMINADO'),(8,2,'Accidente','QA UI reporte 20260607020321',37.4219983,-122.0840567,'BAJO','2026-06-07 07:03:27','VALIDADO'),(9,2,'Asalto','QA ALERT UI 20260607020510',37.4219999,-122.0840575,'ALTO','2026-06-07 07:05:10','ELIMINADO'),(10,3,'Robo a mano armada','Test',37.4219983,-122.0840000,'ALTO','2026-06-07 18:51:29','VALIDADO'),(11,10,'Robo a mano armada','Reporte de prueba en Plaza Mayor: robo a mano armada.',-12.0463740,-77.0427930,'ALTO','2026-06-07 19:10:28','VALIDADO'),(12,10,'Asalto','Reporte de prueba en Abancay: asalto.',-12.0509000,-77.0346000,'ALTO','2026-06-07 19:10:30','VALIDADO'),(13,10,'Secuestro','Reporte de prueba cerca de Palacio de Justicia: secuestro.',-12.0556000,-77.0419000,'ALTO','2026-06-07 19:10:32','ELIMINADO'),(14,10,'Robo','Reporte de prueba en Paseo Colon: robo.',-12.0639000,-77.0375000,'MEDIO','2026-06-07 19:10:35','VALIDADO'),(15,10,'Vandalismo','Reporte de prueba en Parque de la Exposicion: vandalismo.',-12.0691000,-77.0347000,'MEDIO','2026-06-07 19:10:37','ELIMINADO'),(16,10,'Acoso','Reporte de prueba en Arequipa: acoso.',-12.0759000,-77.0325000,'MEDIO','2026-06-07 19:10:39','VALIDADO'),(17,10,'Sospechoso','Reporte de prueba en Jesus Maria: actividad sospechosa.',-12.0889000,-77.0502000,'BAJO','2026-06-07 19:10:41','VALIDADO'),(18,10,'Accidente','Reporte de prueba en Lince: accidente.',-12.0972000,-77.0364000,'BAJO','2026-06-07 19:10:43','VALIDADO');
/*!40000 ALTER TABLE `incidentes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `correo` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contrasena` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `telefono` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `token_fcm` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rol` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'USUARIO',
  `fecha_registro` datetime DEFAULT CURRENT_TIMESTAMP,
  `estado` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVO',
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `correo` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Test','test@test.com','$2a$10$SFO03ZgqAgB4m8IYW1tXxuycS.mk/g2W.BSl8Y3zYrAtoy/F9DXeW',NULL,NULL,'USUARIO','2026-06-06 22:48:23','ACTIVO'),(2,'Test User','test2@test.com','$2a$10$bMk.HwOPqpTZejekQkgSIuDqfBm8eP7PmFAGhOZyLpZ2qA6yPl.76',NULL,'eNYogAp2QvOzotPXfBcEr5:APA91bGZRSZf8E4pq5EX0xQ5zJYMc0ryxIbbDy_xpGlQ9X6zgxC9Bb3je72ruHOb2IctXMFOZ9sDrtkg1i5vNH3VqlVyBUoF63KOFWhZZst_GlL7o87WCyY','USUARIO','2026-06-06 22:48:44','ACTIVO'),(3,'Admin','admin@test.com','$2a$10$BLjWLX75NOFHoGCZvJ4Zoe3aUuUmwH82IACqtuS8NK5MTeS206Unq',NULL,'dB6-JJWOTm-cMRyUl8MEPs:APA91bG6H9CB9HAEM0ZxJ0HN4xrIGJQRnIiWY_pbpazH60ZAD_vBwodGFmFMVjg_u0cMykITxeSv-col4zVaTTaPx8kRxk7Q-RkHkyUCPYhYoljZoovS71Y','USUARIO','2026-06-06 22:52:37','ACTIVO'),(5,'Administrador','admin@zonas.com','$2a$10$FP3UO.ETrXtFQPzynEXesepG.FPS/8Me5uRDhEAq0zE.G.FQ4ZlCG',NULL,'fHkVoFK1TtOvkqGjEubgrW:APA91bHqv8qpgotfJJMO58RVXIANEZGQswWSg4xomboGMtToFdfSeVsTZ3snt38GOzaP-1JKgxD4eZ2yZVtzslcxlOeuj6wIm_aPo-BMCTAbGWNJvax-Tc8','ADMIN','2026-06-06 22:59:59','ACTIVO'),(6,'QA User','qa_20260607014706@test.com','$2a$10$pVfArexfvtZojxufspZ3MurcnrCZq1pTowP2luZRoI7FnLx4tN5A2','999111222',NULL,'USUARIO','2026-06-07 06:47:07','ACTIVO'),(7,'QA User','qa_20260607014943@test.com','$2a$10$OeddqJxyscs9w6esvJLi/usEcaPKgvxeQEzG8MHzrBd6gDVQQRX0q','999111222',NULL,'USUARIO','2026-06-07 06:49:44','ACTIVO'),(8,'QA Registro','qa_ui_reg_202606070207@test.com','$2a$10$9EVvEqTGbusXxX7fgx1UN.gt4KBUfnXc8OvzGr0cApZUwzsIPeVwu','999888777',NULL,'USUARIO','2026-06-07 07:11:05','ACTIVO'),(10,'Usuario Ruta','ruta@test.local','$2a$10$Xdtsj1IOC798n5x2sphrFulNCDx1njoV3AUm3ms7H0KhI1PeUzkmu','999999999',NULL,'USUARIO','2026-06-07 19:09:33','ACTIVO');
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `validacion_reportes`
--

DROP TABLE IF EXISTS `validacion_reportes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `validacion_reportes` (
  `id_validacion` int NOT NULL AUTO_INCREMENT,
  `id_incidente` int DEFAULT NULL,
  `id_usuario` int DEFAULT NULL,
  `accion` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_accion` datetime DEFAULT CURRENT_TIMESTAMP,
  `observacion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_validacion`),
  KEY `id_incidente` (`id_incidente`),
  KEY `id_usuario` (`id_usuario`),
  CONSTRAINT `validacion_reportes_ibfk_1` FOREIGN KEY (`id_incidente`) REFERENCES `incidentes` (`id_incidente`),
  CONSTRAINT `validacion_reportes_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `validacion_reportes`
--

LOCK TABLES `validacion_reportes` WRITE;
/*!40000 ALTER TABLE `validacion_reportes` DISABLE KEYS */;
INSERT INTO `validacion_reportes` VALUES (1,1,5,'VALIDADO','2026-06-06 23:01:54',NULL),(2,2,5,'ELIMINADO','2026-06-06 23:01:54',NULL),(3,4,5,'VALIDADO','2026-06-07 06:47:09',NULL),(4,5,5,'ELIMINADO','2026-06-07 06:47:09',NULL),(5,6,5,'VALIDADO','2026-06-07 06:49:46',NULL),(6,7,5,'ELIMINADO','2026-06-07 06:49:46',NULL),(7,9,5,'ELIMINADO','2026-06-07 07:09:19',NULL),(8,8,5,'VALIDADO','2026-06-07 07:09:22',NULL),(9,3,5,'VALIDADO','2026-06-07 18:54:20',NULL),(10,10,5,'VALIDADO','2026-06-07 18:54:23',NULL),(11,11,5,'VALIDADO','2026-06-07 19:22:58',NULL),(12,12,5,'VALIDADO','2026-06-07 19:23:01',NULL),(13,13,5,'ELIMINADO','2026-06-07 19:23:04',NULL),(14,14,5,'VALIDADO','2026-06-07 19:23:05',NULL),(15,16,5,'VALIDADO','2026-06-07 19:23:08',NULL),(16,17,5,'VALIDADO','2026-06-07 19:23:10',NULL),(17,18,5,'VALIDADO','2026-06-07 19:23:12',NULL),(18,15,5,'ELIMINADO','2026-06-07 19:23:15',NULL);
/*!40000 ALTER TABLE `validacion_reportes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'db_zonas_peligrosas'
--

--
-- Dumping routines for database 'db_zonas_peligrosas'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-07 14:28:03

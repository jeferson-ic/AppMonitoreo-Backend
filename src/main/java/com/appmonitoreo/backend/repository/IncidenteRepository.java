package com.appmonitoreo.backend.repository;

import com.appmonitoreo.backend.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IncidenteRepository extends JpaRepository<Incidente, Integer> {

    List<Incidente> findByUsuarioIdUsuarioAndEstadoNot(Integer idUsuario, String estado);

    long countByEstado(String estado);

    @Query("SELECT i FROM Incidente i WHERE i.estado NOT IN ('ELIMINADO', 'RECHAZADO') " +
           "AND i.latitud BETWEEN :latMin AND :latMax " +
           "AND i.longitud BETWEEN :lngMin AND :lngMax")
    List<Incidente> buscarEnCaja(
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin,
            @Param("lngMax") BigDecimal lngMax);

    @Query("SELECT i FROM Incidente i WHERE i.estado NOT IN ('ELIMINADO', 'RECHAZADO') " +
           "AND i.tipoIncidente = :tipo " +
           "AND i.fechaIncidente >= :desde " +
           "AND i.latitud BETWEEN :latMin AND :latMax " +
           "AND i.longitud BETWEEN :lngMin AND :lngMax")
    List<Incidente> buscarSimilaresParaValidacion(
            @Param("tipo") String tipo,
            @Param("desde") LocalDateTime desde,
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin,
            @Param("lngMax") BigDecimal lngMax);

    @Query("SELECT i FROM Incidente i WHERE i.estado NOT IN ('ELIMINADO', 'RECHAZADO') " +
           "AND (:tipo IS NULL OR i.tipoIncidente = :tipo) " +
           "AND (:nivelRiesgo IS NULL OR i.nivelRiesgo = :nivelRiesgo) " +
           "AND (:fechaDesde IS NULL OR i.fechaIncidente >= :fechaDesde) " +
           "AND (:fechaHasta IS NULL OR i.fechaIncidente <= :fechaHasta) " +
           "ORDER BY i.fechaIncidente DESC")
    List<Incidente> filtrar(
            @Param("tipo") String tipo,
            @Param("nivelRiesgo") String nivelRiesgo,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta);
}

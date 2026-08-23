package com.appmonitoreo.backend.repository;

import com.appmonitoreo.backend.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface IncidenteRepository extends JpaRepository<Incidente, Integer> {

    List<Incidente> findByEstadoNot(String estado);

    List<Incidente> findByUsuarioIdUsuarioAndEstadoNot(Integer idUsuario, String estado);

    @Query("SELECT i FROM Incidente i WHERE i.estado <> 'ELIMINADO' " +
           "AND i.latitud BETWEEN :latMin AND :latMax " +
           "AND i.longitud BETWEEN :lngMin AND :lngMax")
    List<Incidente> buscarEnCaja(
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("lngMin") BigDecimal lngMin,
            @Param("lngMax") BigDecimal lngMax);
}

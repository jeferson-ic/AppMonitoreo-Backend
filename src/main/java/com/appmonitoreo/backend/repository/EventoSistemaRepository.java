package com.appmonitoreo.backend.repository;

import com.appmonitoreo.backend.model.EventoSistema;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoSistemaRepository extends JpaRepository<EventoSistema, Integer> {

    List<EventoSistema> findByTipoOrderByFechaEventoDesc(String tipo, Pageable pageable);

    List<EventoSistema> findAllByOrderByFechaEventoDesc(Pageable pageable);

    long countByTipo(String tipo);
}

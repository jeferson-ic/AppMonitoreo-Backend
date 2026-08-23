package com.appmonitoreo.backend.repository;

import com.appmonitoreo.backend.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {
}

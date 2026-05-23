package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {

    boolean existsBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    void deleteBySeguidorIdAndSeguidoId(Long seguidorId, Long seguidoId);

    List<Seguimiento> findBySeguidoId(Long seguidoId);

    List<Seguimiento> findBySeguidorId(Long seguidorId);

    long countBySeguidoId(Long seguidoId);

    long countBySeguidorId(Long seguidorId);
}

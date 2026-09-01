package br.com.felipe.termometro.catalogo.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface RendaSpringDataJpaRepository extends JpaRepository<RendaJpaEntity, LocalDate> {

    Optional<RendaJpaEntity> findByCompetencia(LocalDate competencia);

    /** Da mais recente para a mais antiga, até {@code ate} inclusive — base da RN-16.1. */
    List<RendaJpaEntity> findByCompetenciaLessThanEqualOrderByCompetenciaDesc(
            LocalDate ate, Pageable pageable);
}

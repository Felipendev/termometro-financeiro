package br.com.felipe.termometro.planilha.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiarioOverrideSpringDataJpaRepository extends JpaRepository<DiarioOverrideJpaEntity, LocalDate> {
    List<DiarioOverrideJpaEntity> findByDataBetween(LocalDate de, LocalDate ate);
    Optional<DiarioOverrideJpaEntity> findFirstByOrderByDataAsc();
}

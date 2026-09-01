package br.com.felipe.termometro.planilha.infra;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservacaoDoDiaSpringDataJpaRepository
        extends JpaRepository<ObservacaoDoDiaJpaEntity, LocalDate> {
    List<ObservacaoDoDiaJpaEntity> findByDataBetween(LocalDate de, LocalDate ate);
}

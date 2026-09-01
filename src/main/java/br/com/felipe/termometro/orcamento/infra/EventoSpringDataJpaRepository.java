package br.com.felipe.termometro.orcamento.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventoSpringDataJpaRepository extends JpaRepository<EventoJpaEntity, UUID> {

    List<EventoJpaEntity> findByCompetenciaOrderByData(LocalDate competencia);
}

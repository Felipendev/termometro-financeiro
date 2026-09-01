package br.com.felipe.termometro.orcamento.infra;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

interface VerbaMensalSpringDataJpaRepository extends JpaRepository<VerbaMensalJpaEntity, LocalDate> {
}

package br.com.felipe.termometro.planilha.infra;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoInicialPlanilhaSpringDataJpaRepository
        extends JpaRepository<SaldoInicialPlanilhaJpaEntity, Short> {
}

package br.com.felipe.termometro.planilha.application.repository;

import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import java.util.Optional;

public interface SaldoInicialRepository {
    Optional<SaldoInicialPlanilha> busca();

    SaldoInicialPlanilha salva(SaldoInicialPlanilha saldoInicial);
}

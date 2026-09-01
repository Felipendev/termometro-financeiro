package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;

/** Saldo declarado pelo usuário para um dia específico — a âncora de onde a cascata parte. */
public record SaldoInicialPlanilha(LocalDate dataReferencia, Dinheiro valor) {

    public SaldoInicialPlanilha {
        Objects.requireNonNull(dataReferencia, "data de referência não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
    }
}

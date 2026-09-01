package br.com.felipe.termometro.contribuicao.domain;

import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/**
 * RN-28 — o objetivo declarado (dízimo ou oferta) e onde a rampa está hoje. A base de cálculo é
 * sempre a renda líquida: o cadastro de renda não distingue bruto de líquido (decisão confirmada
 * com o Felipe, 2026-08-28) — não há valor de "renda bruta" para calcular sobre.
 */
public record MetaContribuicao(
        NomeDaContribuicao nome, Percentual percentualAlvo, Percentual percentualAtual, Percentual passoIncremento) {

    public MetaContribuicao {
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(percentualAlvo, "percentual alvo não pode ser nulo");
        Objects.requireNonNull(percentualAtual, "percentual atual não pode ser nulo");
        Objects.requireNonNull(passoIncremento, "passo de incremento não pode ser nulo");
    }

    public boolean jaAtingiuOAlvo() {
        return percentualAtual.compareTo(percentualAlvo) >= 0;
    }

    public MetaContribuicao comPercentualAtual(Percentual novoPercentualAtual) {
        return new MetaContribuicao(nome, percentualAlvo, novoPercentualAtual, passoIncremento);
    }
}

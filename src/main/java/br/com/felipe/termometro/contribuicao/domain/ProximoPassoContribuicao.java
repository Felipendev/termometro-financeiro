package br.com.felipe.termometro.contribuicao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/** A proposta concreta: "no mês X, dá pra subir de Y% pra Z%, que são R$ W". */
public record ProximoPassoContribuicao(
        NomeDaContribuicao nome, Competencia competencia, Percentual percentualProposto, Dinheiro valorProposto) {

    public ProximoPassoContribuicao {
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(percentualProposto, "percentual proposto não pode ser nulo");
        Objects.requireNonNull(valorProposto, "valor proposto não pode ser nulo");
    }
}

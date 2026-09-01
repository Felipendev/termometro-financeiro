package br.com.felipe.termometro.comparativo.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/** Um lançamento real (ou uma premissa legada) que compõe uma linha do Comparativo. */
public record ItemComparativo(
        GrupoDoComparativo grupo,
        String descricao,
        String categoria,
        Dinheiro valor,
        String origem) {

    public ItemComparativo {
        Objects.requireNonNull(grupo);
        Objects.requireNonNull(descricao);
        Objects.requireNonNull(categoria);
        Objects.requireNonNull(valor);
        Objects.requireNonNull(origem);
    }
}

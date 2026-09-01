package br.com.felipe.termometro.comparativo.domain;

import br.com.felipe.termometro.shared.Percentual;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Um ponto do gráfico: o grupo, quanto ele representa da renda hoje, e — quando calibrado — o
 * que seria bom, ideal e ruim. Todos os três nulos juntos significam "sem meta declarada ainda",
 * nunca "meta é zero". {@code ruim} vem pronto de {@link ReferenciaDoGrupo} (25% acima de
 * {@code bom}, RN-14) — não é recalculado aqui, pra não ter a mesma conta em dois lugares.
 */
public record PontoComparativo(
        GrupoDoComparativo grupo,
        Percentual atual,
        @Nullable Percentual bom,
        @Nullable Percentual ideal,
        @Nullable Percentual ruim,
        Dinheiro valorAtual,
        Dinheiro rendaReferencia,
        String fonte,
        List<ItemComparativo> itens) {

    public PontoComparativo {
        Objects.requireNonNull(grupo, "grupo não pode ser nulo");
        Objects.requireNonNull(atual, "atual não pode ser nulo");
        Objects.requireNonNull(valorAtual, "valor atual não pode ser nulo");
        Objects.requireNonNull(rendaReferencia, "renda de referência não pode ser nula");
        Objects.requireNonNull(fonte, "fonte não pode ser nula");
        itens = List.copyOf(itens);
    }
}

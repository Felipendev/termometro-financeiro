package br.com.felipe.termometro.comparativo.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RN-30 — domínio puro: recebe os itens já nomeados e valorados (custo fixo + piso humano) e a
 * renda líquida, agrupa por {@link GrupoDoComparativo} e devolve um ponto por grupo com movimento
 * no mês. Grupo sem nenhum item declarado simplesmente não aparece — não faz sentido comparar
 * "0% atual" contra uma meta.
 */
public final class CalculadoraDoComparativo {

    private CalculadoraDoComparativo() {
    }

    public static List<PontoComparativo> calcula(Map<String, Dinheiro> valoresPorNomeDeItem, Dinheiro rendaLiquida) {
        Objects.requireNonNull(valoresPorNomeDeItem, "valores não podem ser nulos");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        if (!rendaLiquida.ehPositivo()) {
            return List.of();
        }

        Map<GrupoDoComparativo, Dinheiro> totalPorGrupo = new EnumMap<>(GrupoDoComparativo.class);
        valoresPorNomeDeItem.forEach((nome, valor) -> totalPorGrupo.merge(
                MapeadorDeGrupo.grupoDe(nome), valor, Dinheiro::somar));

        List<PontoComparativo> pontos = new ArrayList<>();
        totalPorGrupo.forEach((grupo, total) -> {
            var referencia = ReferenciaDoGrupo.para(grupo);
            pontos.add(new PontoComparativo(
                    grupo,
                    total.sobre(rendaLiquida),
                    referencia.map(ReferenciaDoGrupo::bom).orElse(null),
                    referencia.map(ReferenciaDoGrupo::ideal).orElse(null),
                    referencia.map(ReferenciaDoGrupo::ruim).orElse(null)));
        });
        return List.copyOf(pontos);
    }
}

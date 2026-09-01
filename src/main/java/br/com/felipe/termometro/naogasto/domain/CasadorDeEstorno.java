package br.com.felipe.termometro.naogasto.domain;

import br.com.felipe.termometro.ingestao.domain.Normalizador;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * RN-03 — "estorno/chargeback: casa com a transação original (mesmo estabelecimento, valor
 * oposto, ≤ 90 dias) e anula ambas." Reaproveita {@link Normalizador#chaveDeEstabelecimento} —
 * mesma chave de agrupamento já usada por vampiros (RN-07) e pela fila de revisão (RN-12) — em
 * vez de inventar outra heurística de "mesmo estabelecimento".
 *
 * <p>Mesmo casamento guloso de {@link CasadorDeTransferenciaPropria}, só que dentro de cada grupo
 * de estabelecimento em vez de entre contas.
 */
public final class CasadorDeEstorno {

    private CasadorDeEstorno() {
    }

    public static Set<UUID> casar(List<LancamentoParaConciliar> lancamentos, int janelaDias) {
        Map<String, List<LancamentoParaConciliar>> porEstabelecimento = lancamentos.stream()
                .collect(Collectors.groupingBy(
                        l -> Normalizador.chaveDeEstabelecimento(l.descricao(), l.cidade())));

        Set<UUID> casados = new HashSet<>();
        for (List<LancamentoParaConciliar> grupo : porEstabelecimento.values()) {
            casados.addAll(casarDentroDoGrupo(grupo, janelaDias));
        }
        return casados;
    }

    private static Set<UUID> casarDentroDoGrupo(List<LancamentoParaConciliar> grupo, int janelaDias) {
        List<LancamentoParaConciliar> saidas = grupo.stream()
                .filter(l -> l.valor().ehNegativo())
                .sorted(Comparator.comparing(LancamentoParaConciliar::data))
                .toList();
        List<LancamentoParaConciliar> entradas = new ArrayList<>(
                grupo.stream().filter(l -> l.valor().ehPositivo()).toList());

        Set<UUID> casados = new HashSet<>();
        for (LancamentoParaConciliar saida : saidas) {
            if (casados.contains(saida.id())) {
                continue;
            }
            LancamentoParaConciliar melhor = melhorParDisponivel(saida, entradas, casados, janelaDias);
            if (melhor != null) {
                casados.add(saida.id());
                casados.add(melhor.id());
            }
        }
        return casados;
    }

    private static @Nullable LancamentoParaConciliar melhorParDisponivel(
            LancamentoParaConciliar saida, List<LancamentoParaConciliar> entradas, Set<UUID> casados,
            int janelaDias) {
        LancamentoParaConciliar melhor = null;
        long menorDiferenca = Long.MAX_VALUE;
        for (LancamentoParaConciliar entrada : entradas) {
            if (casados.contains(entrada.id())) {
                continue;
            }
            if (!entrada.valor().equals(saida.valor().negado())) {
                continue;
            }
            long diferenca = Math.abs(ChronoUnit.DAYS.between(saida.data(), entrada.data()));
            if (diferenca > janelaDias) {
                continue;
            }
            if (diferenca < menorDiferenca) {
                menorDiferenca = diferenca;
                melhor = entrada;
            }
        }
        return melhor;
    }
}

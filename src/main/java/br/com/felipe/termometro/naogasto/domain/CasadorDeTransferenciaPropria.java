package br.com.felipe.termometro.naogasto.domain;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * RN-03 — "transferência entre contas próprias: casada por valor + data (±1 dia) + contraparte
 * pertencente ao usuário. Anula ambas." Este código é single-tenant: toda conta sincronizada é do
 * Felipe, então "contraparte própria" se reduz a "veio de uma {@code identificadorConta}
 * diferente" — não há outra pessoa cujas contas pudessem aparecer aqui.
 *
 * <p>Casamento guloso, não ótimo global: processa as saídas em ordem de data e escolhe, entre as
 * entradas ainda livres de valor oposto exato numa conta diferente, a de data mais próxima.
 * Suficiente no volume de dados de uma pessoa física — duas transferências idênticas no mesmo dia
 * (ex. duas de R$ 500 pra poupança) casam cada uma com seu par mais próximo, uma de cada vez.
 */
public final class CasadorDeTransferenciaPropria {

    private CasadorDeTransferenciaPropria() {
    }

    public static Set<UUID> casar(List<LancamentoParaConciliar> lancamentos, int janelaDias) {
        List<LancamentoParaConciliar> saidas = lancamentos.stream()
                .filter(l -> l.valor().ehNegativo())
                .sorted(Comparator.comparing(LancamentoParaConciliar::data))
                .toList();
        List<LancamentoParaConciliar> entradas =
                lancamentos.stream().filter(l -> l.valor().ehPositivo()).toList();

        Set<UUID> casados = new HashSet<>();
        for (LancamentoParaConciliar saida : saidas) {
            if (casados.contains(saida.id())) {
                continue;
            }
            LancamentoParaConciliar melhorPar = melhorParDisponivel(saida, entradas, casados, janelaDias);
            if (melhorPar != null) {
                casados.add(saida.id());
                casados.add(melhorPar.id());
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
            if (entrada.identificadorConta().equals(saida.identificadorConta())) {
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

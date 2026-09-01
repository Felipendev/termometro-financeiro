package br.com.felipe.termometro.naogasto.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RN-03 — "o débito na conta corrente que quita a fatura não é despesa; a despesa são as
 * transações dentro da fatura". Só o débito na corrente vira {@code ignorada}; as transações do
 * cartão continuam como estão — são elas o gasto real que a triagem (RN-05) precisa ver.
 *
 * <p><b>Aproximação documentada:</b> a spec não define os dias exatos de fechamento/vencimento de
 * cada cartão, e este código não tem um conceito de "fatura" (ciclo de fechamento) persistido —
 * só lançamentos soltos. O total de uma fatura é aproximado pela soma dos lançamentos de uma
 * conta de cartão dentro de um mês-calendário ({@link Competencia}), e o pagamento é procurado na
 * conta corrente dentro de {@code janelaDiasAposFechamento} dias a partir do primeiro dia do mês
 * seguinte. Não inventa uma fórmula de vencimento sem dado real — quando o débito não bate dentro
 * da tolerância e da janela, simplesmente não casa.
 */
public final class CasadorDePagamentoDeFatura {

    private CasadorDePagamentoDeFatura() {
    }

    public static Set<UUID> casar(List<LancamentoParaConciliar> lancamentos, Dinheiro tolerancia,
            int janelaDiasAposFechamento) {
        Map<String, Dinheiro> totalDaFaturaPorContaECompetencia = new HashMap<>();
        for (LancamentoParaConciliar l : lancamentos) {
            if (l.veioDeFaturaDeCartao() && l.secao().compoeTotal()) {
                String chave = chaveDaFatura(l.identificadorConta(), Competencia.de(l.data()));
                totalDaFaturaPorContaECompetencia.merge(chave, l.valor(), Dinheiro::somar);
            }
        }

        List<LancamentoParaConciliar> debitosNaCorrente = lancamentos.stream()
                .filter(LancamentoParaConciliar::veioDeContaCorrente)
                .filter(l -> l.valor().ehNegativo())
                .sorted((a, b) -> a.data().compareTo(b.data()))
                .toList();

        Set<UUID> casados = new HashSet<>();
        for (Map.Entry<String, Dinheiro> fatura : totalDaFaturaPorContaECompetencia.entrySet()) {
            Competencia competenciaDaFatura = competenciaDaChave(fatura.getKey());
            Competencia competenciaDoPagamento = competenciaDaFatura.mais(1);
            var inicioDaJanela = competenciaDoPagamento.primeiroDia();
            var fimDaJanela = inicioDaJanela.plusDays(janelaDiasAposFechamento - 1L);

            for (LancamentoParaConciliar debito : debitosNaCorrente) {
                if (casados.contains(debito.id())) {
                    continue;
                }
                if (debito.data().isBefore(inicioDaJanela) || debito.data().isAfter(fimDaJanela)) {
                    continue;
                }
                // Ambos são despesa (RN-01: saída negativa) — a soma da fatura e o débito que a
                // paga têm o MESMO sinal e a mesma ordem de grandeza; não há inversão de sinal aqui.
                Dinheiro diferenca = debito.valor().subtrair(fatura.getValue()).absoluto();
                if (diferenca.menorOuIgualA(tolerancia)) {
                    casados.add(debito.id());
                    break;
                }
            }
        }
        return casados;
    }

    private static String chaveDaFatura(String identificadorConta, Competencia competencia) {
        return identificadorConta + "|" + competencia;
    }

    private static Competencia competenciaDaChave(String chave) {
        return Competencia.parse(chave.substring(chave.indexOf('|') + 1));
    }
}

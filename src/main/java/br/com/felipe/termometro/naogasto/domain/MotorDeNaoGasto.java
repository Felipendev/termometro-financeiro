package br.com.felipe.termometro.naogasto.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RN-03 — orquestra os três casadores nesta ordem de prioridade: pagamento de fatura,
 * transferência própria, estorno. Um lançamento já casado por um motor sai do conjunto candidato
 * dos seguintes, para que o mesmo débito não seja reivindicado duas vezes por motivos diferentes.
 */
public final class MotorDeNaoGasto {

    private MotorDeNaoGasto() {
    }

    public static ResultadoDaConciliacao concilia(List<LancamentoParaConciliar> lancamentos,
            Dinheiro toleranciaFatura, int janelaFaturaDias, int janelaTransferenciaDias,
            int janelaEstornoDias) {
        Set<UUID> pagamentos =
                CasadorDePagamentoDeFatura.casar(lancamentos, toleranciaFatura, janelaFaturaDias);
        List<LancamentoParaConciliar> restantes1 = semCasados(lancamentos, pagamentos);

        Set<UUID> transferencias =
                CasadorDeTransferenciaPropria.casar(restantes1, janelaTransferenciaDias);
        List<LancamentoParaConciliar> restantes2 = semCasados(restantes1, transferencias);

        Set<UUID> estornos = CasadorDeEstorno.casar(restantes2, janelaEstornoDias);

        Set<UUID> todos = new HashSet<>();
        todos.addAll(pagamentos);
        todos.addAll(transferencias);
        todos.addAll(estornos);

        Map<UUID, LancamentoParaConciliar> porId = lancamentos.stream()
                .collect(Collectors.toMap(LancamentoParaConciliar::id, l -> l));
        Dinheiro valorTotal = Dinheiro.somaDe(
                todos.stream().map(id -> porId.get(id).valor().absoluto()).toList());

        List<String> detalhes = new ArrayList<>();
        if (!pagamentos.isEmpty()) {
            detalhes.add(pagamentos.size() + " débito(s) de pagamento de fatura reconhecido(s).");
        }
        if (!transferencias.isEmpty()) {
            detalhes.add((transferencias.size() / 2)
                    + " transferência(s) entre contas próprias reconhecida(s).");
        }
        if (!estornos.isEmpty()) {
            detalhes.add((estornos.size() / 2) + " estorno(s) casado(s) com a compra original.");
        }

        return new ResultadoDaConciliacao(
                todos, pagamentos.size(), transferencias.size() / 2, estornos.size() / 2, valorTotal, detalhes);
    }

    private static List<LancamentoParaConciliar> semCasados(
            List<LancamentoParaConciliar> lancamentos, Set<UUID> casados) {
        return lancamentos.stream().filter(l -> !casados.contains(l.id())).toList();
    }
}

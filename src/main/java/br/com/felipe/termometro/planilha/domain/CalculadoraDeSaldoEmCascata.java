package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * O coração da planilha viva (RN-24.1): soma passo a passo, dia após dia, sem pular nenhum — é
 * essa cascata que faz o saldo de um mês virar o ponto de partida do mês seguinte, exatamente
 * como a coluna Saldo da planilha em Excel sempre funcionou.
 *
 * <p>De caminho, classifica o uso de crédito (RN-18) de cada saída de cartão: o saldo parcial
 * dentro do próprio dia, item a item, é exatamente o que a Sinal 2 precisa — "a situação de
 * caixa antes desta transação específica", não a do mês nem a de hoje.
 *
 * <p><b>Limite conhecido:</b> sem hora confiável na maioria das fontes (PDF não traz), a ordem
 * dos itens dentro do dia é a ordem em que chegaram na lista, não a ordem real dos relógios —
 * a classificação de Sinal 2 herda essa aproximação.
 *
 * <p>Domínio puro: recebe os lançamentos já agrupados por dia (quem os monta é a camada de
 * aplicação, que sabe onde cada um mora — lançamento planejado ou transação importada). Esta
 * classe não conhece banco nem competência, só sabe somar em ordem.
 */
public final class CalculadoraDeSaldoEmCascata {

    private CalculadoraDeSaldoEmCascata() {
    }

    public static List<DiaDaPlanilha> calcula(
            List<LocalDate> dias,
            Map<LocalDate, List<ItemDoDia>> lancamentosPorDia,
            Map<LocalDate, Dinheiro> diariosSobrescritos,
            Map<LocalDate, String> observacoes,
            Dinheiro saldoInicial) {
        Objects.requireNonNull(dias, "dias não podem ser nulos");
        Objects.requireNonNull(lancamentosPorDia, "lançamentos não podem ser nulos");
        Objects.requireNonNull(diariosSobrescritos, "diários não podem ser nulos");
        Objects.requireNonNull(observacoes, "observações não podem ser nulas");
        Objects.requireNonNull(saldoInicial, "saldo inicial não pode ser nulo");

        List<DiaDaPlanilha> resultado = new ArrayList<>(dias.size());
        Dinheiro saldoCorrente = saldoInicial;
        for (LocalDate dia : dias) {
            List<ItemDoDia> lancamentosDoDia = lancamentosPorDia.getOrDefault(dia, List.of());
            Dinheiro diario = diariosSobrescritos.getOrDefault(dia, Dinheiro.ZERO);
            List<ItemDoDia> lancamentosClassificados = classificaUsoDeCredito(lancamentosDoDia, saldoCorrente);

            Dinheiro entrada = somaPorTipo(lancamentosClassificados, TipoItemDoDia.ENTRADA);
            Dinheiro saida = somaPorTipo(lancamentosClassificados, TipoItemDoDia.SAIDA);
            saldoCorrente = saldoCorrente.somar(entrada).subtrair(saida).subtrair(diario);

            resultado.add(new DiaDaPlanilha(dia, dia.getDayOfWeek(), lancamentosClassificados, diario,
                    diariosSobrescritos.containsKey(dia), saldoCorrente, observacoes.get(dia)));
        }
        return List.copyOf(resultado);
    }

    private static List<ItemDoDia> classificaUsoDeCredito(List<ItemDoDia> lancamentosDoDia, Dinheiro saldoAntesDoDia) {
        List<ItemDoDia> classificados = new ArrayList<>(lancamentosDoDia.size());
        Dinheiro saldoParcial = saldoAntesDoDia;
        for (ItemDoDia item : lancamentosDoDia) {
            if (item.tipo() == TipoItemDoDia.SAIDA) {
                classificados.add(item.vemDeCartao()
                        ? item.comUsoDeCredito(ClassificadorDeUsoDeCredito.classifica(item.descricao(), saldoParcial))
                        : item);
                saldoParcial = saldoParcial.subtrair(item.valor());
            } else {
                classificados.add(item);
                saldoParcial = saldoParcial.somar(item.valor());
            }
        }
        return classificados;
    }

    private static Dinheiro somaPorTipo(List<ItemDoDia> lancamentos, TipoItemDoDia tipo) {
        return Dinheiro.somaDe(lancamentos.stream()
                .filter(item -> item.tipo() == tipo)
                .map(ItemDoDia::valor)
                .toList());
    }
}

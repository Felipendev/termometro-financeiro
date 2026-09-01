package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;

/** O mês inteiro da planilha — os dias já em cascata, mais os totais que fecham a coluna. */
public record PlanilhaDoMes(
        Competencia competencia,
        List<DiaDaPlanilha> dias,
        Dinheiro totalEntrada,
        Dinheiro totalSaida,
        Dinheiro totalDiario,
        Dinheiro saldoFinal,
        Dinheiro totalDeficitDisfarcado,
        int transacoesEmAtencao) {

    public PlanilhaDoMes {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(dias, "dias não podem ser nulos");
        dias = List.copyOf(dias);
    }

    public static PlanilhaDoMes de(Competencia competencia, List<DiaDaPlanilha> dias) {
        List<ItemDoDia> todosOsItens = dias.stream().flatMap(dia -> dia.lancamentos().stream()).toList();

        Dinheiro totalEntrada = Dinheiro.somaDe(dias.stream().map(DiaDaPlanilha::entrada).toList());
        Dinheiro totalSaida = Dinheiro.somaDe(dias.stream().map(DiaDaPlanilha::saida).toList());
        Dinheiro totalDiario = Dinheiro.somaDe(dias.stream().map(DiaDaPlanilha::diario).toList());
        Dinheiro saldoFinal = dias.isEmpty() ? Dinheiro.ZERO : dias.get(dias.size() - 1).saldo();

        Dinheiro totalDeficitDisfarcado = Dinheiro.somaDe(todosOsItens.stream()
                .filter(item -> item.usoDeCredito() == UsoDeCredito.DEFICIT_DISFARCADO)
                .map(ItemDoDia::valor)
                .toList());
        long transacoesEmAtencao = todosOsItens.stream()
                .filter(item -> item.usoDeCredito() == UsoDeCredito.ATENCAO)
                .count();

        return new PlanilhaDoMes(competencia, dias, totalEntrada, totalSaida, totalDiario, saldoFinal,
                totalDeficitDisfarcado, (int) transacoesEmAtencao);
    }
}

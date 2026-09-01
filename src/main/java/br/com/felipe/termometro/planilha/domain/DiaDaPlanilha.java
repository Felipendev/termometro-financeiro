package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Uma célula-linha da planilha viva (RN-24.1). Entrada e saída nunca são digitadas aqui — são
 * sempre a soma de {@link #lancamentos()}, banco ou manual, exatamente como o Felipe pediu: a
 * composição é a fonte da verdade, o total é só o que se mostra por cima.
 */
public record DiaDaPlanilha(
        LocalDate data,
        DayOfWeek diaDaSemana,
        List<ItemDoDia> lancamentos,
        Dinheiro diario,
        boolean diarioSobrescrito,
        Dinheiro saldo,
        @Nullable String observacao) {

    public DiaDaPlanilha {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(diaDaSemana, "dia da semana não pode ser nulo");
        Objects.requireNonNull(lancamentos, "lançamentos não podem ser nulos");
        Objects.requireNonNull(diario, "diário não pode ser nulo");
        Objects.requireNonNull(saldo, "saldo não pode ser nulo");
        lancamentos = List.copyOf(lancamentos);
    }

    public Dinheiro entrada() {
        return somaPorTipo(TipoItemDoDia.ENTRADA);
    }

    public Dinheiro saida() {
        return somaPorTipo(TipoItemDoDia.SAIDA);
    }

    public FaixaDeSaldo faixaSaldo() {
        return FaixaDeSaldo.de(saldo);
    }

    private Dinheiro somaPorTipo(TipoItemDoDia tipo) {
        return Dinheiro.somaDe(lancamentos.stream()
                .filter(item -> item.tipo() == tipo)
                .map(ItemDoDia::valor)
                .toList());
    }
}

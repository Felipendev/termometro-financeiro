package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Gasto variável agregado por dia, já em valor positivo (é despesa, o sinal da RN-01 fica na
 * camada de transação) e já sem o que estiver vinculado a um {@link Evento} — evento tem
 * orçamento próprio na provisão e contá-lo aqui seria contar duas vezes.
 */
public record GastoDoDia(LocalDate data, Dinheiro valor) {

    public GastoDoDia {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (valor.ehNegativo()) {
            throw new IllegalArgumentException("gasto do dia é positivo aqui: " + valor);
        }
    }
}

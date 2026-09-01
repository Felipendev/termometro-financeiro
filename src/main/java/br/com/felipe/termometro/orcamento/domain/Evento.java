package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Gasto irregular com data e valor conhecidos: a viagem de sábado, o evento do mês que vem
 * (RN-20).
 *
 * <p>Projetar o mês perfeito é ficção, e ficção quebra na primeira semana. O usuário <i>vai</i>
 * pegar o Uber de R$ 55 e <i>vai</i> ao evento de R$ 170 — isso não é descontrole, é vida. Evento
 * conhecido entra no orçamento antes de acontecer, não depois.
 *
 * @param realizado se o gasto já aconteceu (e portanto já aparece nas transações do mês)
 */
public record Evento(LocalDate data, String descricao, Dinheiro valor, boolean realizado) {

    public Evento {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (valor.ehNegativo()) {
            throw new IllegalArgumentException("evento tem valor positivo: " + valor);
        }
    }

    public static Evento previsto(LocalDate data, String descricao, Dinheiro valor) {
        return new Evento(data, descricao, valor, false);
    }

    public boolean aindaVaiAcontecer(LocalDate hoje) {
        return !realizado && !data.isBefore(hoje);
    }
}

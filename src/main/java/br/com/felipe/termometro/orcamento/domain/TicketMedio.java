package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;

/**
 * Ticket médio medido nas transações do próprio usuário — a unidade em que a verba vira ação.
 *
 * <p>Os valores padrão vêm das faturas reais de junho a agosto de 2026 e são substituídos assim
 * que houver histórico suficiente. Genérico não serve: R$ 91 é "duas refeições" para quem gasta
 * R$ 38 por refeição e "uma" para quem gasta R$ 80.
 */
public record TicketMedio(String singular, String plural, Dinheiro valor) {

    public TicketMedio {
        Objects.requireNonNull(singular, "singular não pode ser nulo");
        Objects.requireNonNull(plural, "plural não pode ser nulo");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("ticket médio precisa ser positivo: " + valor);
        }
    }

    /**
     * Medidos em 200 transações das faturas de jun–ago/2026.
     *
     * <p>Singular e plural são declarados, não derivados: "refeição fora" não vira "refeição foras"
     * com um {@code + "s"}, e uma frase com erro de português numa notificação diária mina a
     * confiança no resto do número.
     */
    public static List<TicketMedio> medidosEmAgosto2026() {
        return List.of(
                new TicketMedio("refeição fora", "refeições fora", Dinheiro.de("38.16")),
                new TicketMedio("mercado", "mercados", Dinheiro.de("43.79")),
                new TicketMedio("pedido de delivery", "pedidos de delivery", Dinheiro.de("34.20")),
                new TicketMedio("corrida de app", "corridas de app", Dinheiro.de("8.63")));
    }
}

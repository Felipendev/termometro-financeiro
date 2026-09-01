package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * A verba traduzida em coisas que se faz (RN-19).
 *
 * <p>"Você tem R$ 91,67 hoje" não muda comportamento. "Dá para 2 refeições fora" muda.
 */
public record AcaoPossivel(String singular, String plural, Dinheiro ticketMedio, int quantidade) {

    public AcaoPossivel {
        Objects.requireNonNull(singular, "singular não pode ser nulo");
        Objects.requireNonNull(plural, "plural não pode ser nulo");
        Objects.requireNonNull(ticketMedio, "ticket não pode ser nulo");
        if (quantidade < 1) {
            throw new IllegalArgumentException("ação possível tem quantidade ≥ 1: " + quantidade);
        }
    }

    public static AcaoPossivel de(TicketMedio ticket, int quantidade) {
        return new AcaoPossivel(ticket.singular(), ticket.plural(), ticket.valor(), quantidade);
    }

    /** {@code "3 refeições fora de R$ 38,16"} */
    public String frase() {
        return quantidade + " " + (quantidade == 1 ? singular : plural) + " de " + ticketMedio;
    }

    @Override
    public String toString() {
        return frase();
    }
}

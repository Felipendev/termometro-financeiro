package br.com.felipe.termometro.orcamento.application.api.response;

import br.com.felipe.termometro.orcamento.domain.AcaoPossivel;
import br.com.felipe.termometro.shared.Dinheiro;

/** A verba traduzida em algo que se faz: {@code "3 refeições fora de R$ 38,16"}. */
public record AcaoPossivelResponse(String categoria, int quantidade, Dinheiro ticketMedio, String frase) {

    public AcaoPossivelResponse(AcaoPossivel acao) {
        this(acao.plural(), acao.quantidade(), acao.ticketMedio(), acao.frase());
    }
}

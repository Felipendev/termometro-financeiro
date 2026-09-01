package br.com.felipe.termometro.cartao.application.api.response;

import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;

public record CartaoResponse(
        UUID id,
        String nome,
        Dinheiro limite,
        Dinheiro valorFatura,
        String observacao) {

    public CartaoResponse(Cartao cartao) {
        this(cartao.id(), cartao.nome(), cartao.limite(), cartao.valorFatura(), cartao.observacao());
    }
}

package br.com.felipe.termometro.catalogo.application.api.response;

import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;

public record CustoFixoItemResponse(UUID id, String nome, Dinheiro valor, String formaPagamento,
                                     String observacao) {

    public CustoFixoItemResponse(CustoFixoItem item) {
        this(item.id(), item.nome(), item.valor(), item.formaPagamento(), item.observacao());
    }
}

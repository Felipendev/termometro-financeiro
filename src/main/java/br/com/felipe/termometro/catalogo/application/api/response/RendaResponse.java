package br.com.felipe.termometro.catalogo.application.api.response;

import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Dinheiro;

public record RendaResponse(String competencia, Dinheiro valorLiquido, String observacao) {

    public RendaResponse(Renda renda) {
        this(renda.competencia().toString(), renda.valorLiquido(), renda.observacao());
    }
}

package br.com.felipe.termometro.triagem.application.api.response;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.domain.ResumoDeCategoria;

public record ResumoDeCategoriaResponse(
        String categoria, String natureza,
        Dinheiro totalAzul, Dinheiro totalAmarelo, Dinheiro totalVermelho, Dinheiro totalVerde,
        Dinheiro totalNaoTriada) {

    public ResumoDeCategoriaResponse(ResumoDeCategoria resumo) {
        this(resumo.categoria(), resumo.natureza().name(), resumo.totalAzul(), resumo.totalAmarelo(),
                resumo.totalVermelho(), resumo.totalVerde(), resumo.totalNaoTriada());
    }
}

package br.com.felipe.termometro.contribuicao.application.api.response;

import br.com.felipe.termometro.contribuicao.application.service.MetaComProximoPasso;
import org.jspecify.annotations.Nullable;

public record MetaContribuicaoResponse(
        String nome,
        String percentualAtual,
        String percentualAlvo,
        @Nullable String valorMensalAtual,
        @Nullable ProximoPassoResponse proximoPassoSugerido,
        @Nullable String informacaoNecessaria) {

    public static MetaContribuicaoResponse de(MetaComProximoPasso item) {
        var meta = item.meta();
        return new MetaContribuicaoResponse(
                meta.nome().name(),
                meta.percentualAtual().paraJson(),
                meta.percentualAlvo().paraJson(),
                item.valorMensalAtual() == null ? null : item.valorMensalAtual().paraJson(),
                item.proximoPasso() == null ? null : ProximoPassoResponse.de(item.proximoPasso()),
                item.informacaoNecessaria());
    }
}

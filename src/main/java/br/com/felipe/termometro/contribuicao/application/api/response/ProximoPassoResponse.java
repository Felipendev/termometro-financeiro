package br.com.felipe.termometro.contribuicao.application.api.response;

import br.com.felipe.termometro.contribuicao.domain.ProximoPassoContribuicao;

public record ProximoPassoResponse(String competencia, String percentualProposto, String valorProposto) {

    public static ProximoPassoResponse de(ProximoPassoContribuicao passo) {
        return new ProximoPassoResponse(
                passo.competencia().toString(), passo.percentualProposto().paraJson(), passo.valorProposto().paraJson());
    }
}

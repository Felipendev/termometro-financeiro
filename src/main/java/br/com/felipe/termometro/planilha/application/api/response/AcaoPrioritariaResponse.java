package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.AcaoPrioritaria;

public record AcaoPrioritariaResponse(String categoria, String descricao, String economiaMensal, int dor) {

    public static AcaoPrioritariaResponse de(AcaoPrioritaria acao) {
        return new AcaoPrioritariaResponse(acao.categoria(), acao.descricao(), acao.economiaMensal().paraJson(), acao.dor());
    }
}

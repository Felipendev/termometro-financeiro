package br.com.felipe.termometro.planoajuste.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.AcaoPrioritaria;
import java.math.BigDecimal;

public record AcaoPrioritariaResponse(
        String categoria, String descricao, BigDecimal economiaMensal, int dor, BigDecimal impacto) {

    public static AcaoPrioritariaResponse de(AcaoPrioritaria acao) {
        return new AcaoPrioritariaResponse(acao.categoria(), acao.descricao(), acao.economiaMensal().valor(),
                acao.dor(), acao.impacto());
    }
}

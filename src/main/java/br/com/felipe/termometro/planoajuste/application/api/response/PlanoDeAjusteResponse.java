package br.com.felipe.termometro.planoajuste.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import java.math.BigDecimal;
import java.util.List;

public record PlanoDeAjusteResponse(
        String competenciaInicio, List<ItemDoPlanoResponse> itens, List<String> avisos,
        List<AcaoPrioritariaResponse> acoesPrioritarias, BigDecimal economiaMensalFinalTotal) {

    public static PlanoDeAjusteResponse de(PlanoDeAjuste plano) {
        return new PlanoDeAjusteResponse(plano.competenciaInicio().toString(),
                plano.itens().stream().map(ItemDoPlanoResponse::de).toList(), plano.avisos(),
                plano.acoesPrioritarias().stream().map(AcaoPrioritariaResponse::de).toList(),
                plano.economiaMensalFinalTotal().valor());
    }
}

package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import java.util.List;

/** RN-23.3 — só a parte acionável do plano de ajuste (RN-15), no ponto exato da decisão. */
public record PriorizacaoResponse(
        String competenciaInicio,
        List<AcaoPrioritariaResponse> acoesPrioritarias,
        String economiaMensalFinalTotal,
        List<String> avisos) {

    public static PriorizacaoResponse de(PlanoDeAjuste plano) {
        return new PriorizacaoResponse(
                plano.competenciaInicio().toString(),
                plano.acoesPrioritarias().stream().map(AcaoPrioritariaResponse::de).toList(),
                plano.economiaMensalFinalTotal().paraJson(),
                plano.avisos());
    }
}

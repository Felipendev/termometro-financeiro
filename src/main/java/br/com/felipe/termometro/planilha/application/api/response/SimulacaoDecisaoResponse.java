package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planilha.application.service.ResultadoDaSimulacao;
import java.util.List;

public record SimulacaoDecisaoResponse(
        List<PlanilhaMesResponse> cenarioReal,
        List<PlanilhaMesResponse> cenarioSimulado,
        String usoDeCreditoPrevisto,
        PriorizacaoResponse priorizacaoSeDeficit) {

    public static SimulacaoDecisaoResponse de(ResultadoDaSimulacao resultado) {
        return new SimulacaoDecisaoResponse(
                resultado.cenarioReal().stream().map(PlanilhaMesResponse::de).toList(),
                resultado.cenarioSimulado().stream().map(PlanilhaMesResponse::de).toList(),
                resultado.usoDeCreditoPrevisto() == null ? null : resultado.usoDeCreditoPrevisto().name(),
                resultado.priorizacaoSeDeficit() == null ? null : PriorizacaoResponse.de(resultado.priorizacaoSeDeficit()));
    }
}

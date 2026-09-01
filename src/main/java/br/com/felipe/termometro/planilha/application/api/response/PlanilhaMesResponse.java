package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import java.util.List;

public record PlanilhaMesResponse(
        String competencia,
        List<DiaDaPlanilhaResponse> dias,
        String totalEntrada,
        String totalSaida,
        String totalDiario,
        String saldoFinal,
        String totalDeficitDisfarcado,
        int transacoesEmAtencao) {

    public static PlanilhaMesResponse de(PlanilhaDoMes planilha) {
        return new PlanilhaMesResponse(
                planilha.competencia().toString(),
                planilha.dias().stream().map(DiaDaPlanilhaResponse::de).toList(),
                planilha.totalEntrada().paraJson(),
                planilha.totalSaida().paraJson(),
                planilha.totalDiario().paraJson(),
                planilha.saldoFinal().paraJson(),
                planilha.totalDeficitDisfarcado().paraJson(),
                planilha.transacoesEmAtencao());
    }
}

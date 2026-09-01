package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import br.com.felipe.termometro.planilha.domain.UsoDeCredito;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * RN-23.1 — o que a simulação devolve, lado a lado, sem persistir nada. {@code priorizacaoSeDeficit}
 * só vem preenchido quando algum mês do cenário simulado fecha negativo (RN-23.3).
 */
public record ResultadoDaSimulacao(
        List<PlanilhaDoMes> cenarioReal,
        List<PlanilhaDoMes> cenarioSimulado,
        @Nullable UsoDeCredito usoDeCreditoPrevisto,
        @Nullable PlanoDeAjuste priorizacaoSeDeficit) {
}

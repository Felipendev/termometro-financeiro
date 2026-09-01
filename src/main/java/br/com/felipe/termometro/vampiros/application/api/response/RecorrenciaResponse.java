package br.com.felipe.termometro.vampiros.application.api.response;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import br.com.felipe.termometro.vampiros.domain.Periodicidade;
import br.com.felipe.termometro.vampiros.domain.Recorrencia;

/** RN-07 — a frase-alvo da spec: "cancelar libera R$ X/mês, R$ Y/ano". */
public record RecorrenciaResponse(
        String nomeNormalizado,
        Periodicidade periodicidade,
        Dinheiro valorMedio,
        Dinheiro custoAnual,
        Percentual confianca,
        String primeiraOcorrencia,
        String ultimaOcorrencia,
        int ocorrencias,
        boolean reajusteDetectado,
        boolean cobrancaSilenciosa,
        String mensagem) {

    public RecorrenciaResponse(Recorrencia recorrencia) {
        this(recorrencia.nomeNormalizado(), recorrencia.periodicidade(), recorrencia.valorMedio(),
                recorrencia.custoAnual(), recorrencia.confianca(), recorrencia.primeiraOcorrencia().toString(),
                recorrencia.ultimaOcorrencia().toString(), recorrencia.ocorrencias(),
                recorrencia.reajusteDetectado(), recorrencia.cobrancaSilenciosa(), mensagem(recorrencia));
    }

    private static String mensagem(Recorrencia recorrencia) {
        return recorrencia.periodicidade() == Periodicidade.MENSAL
                ? "Cancelar libera " + recorrencia.valorMedio() + "/mês, " + recorrencia.custoAnual() + "/ano."
                : "Cancelar libera " + recorrencia.custoAnual() + "/ano.";
    }
}

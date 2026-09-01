package br.com.felipe.termometro.classificacao.application.api.response;

import java.util.List;
import java.util.Map;

/**
 * @param naVerbaDiaria    quantas passaram a contar na verba do dia (RN-19)
 * @param pendentesRevisao quantas o sistema não decidiu sozinho (RN-12)
 * @param porCategoria     quantas transações por categoria, para conferir a calibração de olho
 */
public record ResultadoDaClassificacaoResponse(
        String competencia,
        int analisadas,
        int classificadas,
        int naVerbaDiaria,
        int pendentesRevisao,
        Map<String, Integer> porCategoria,
        List<String> avisos) {
}

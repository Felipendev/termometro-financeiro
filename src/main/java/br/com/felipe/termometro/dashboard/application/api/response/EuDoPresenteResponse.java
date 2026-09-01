package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.cartao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.diagnostico.application.api.response.SaldoDeSobrevivenciaResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.vampiros.application.api.response.RecorrenciaResponse;
import java.util.List;

/**
 * O mês corrente: diagnóstico (RN-08), resumo da triagem por categoria/cor (RN-05) e vampiros
 * (RN-07). "Pendentes de decisão" da spec não existe de fato — {@code PATCH /vampiros/{id}} nunca
 * foi implementado (ver Javadoc de {@code VampirosAPI}), então todo vampiro devolvido pela
 * consulta já é, por definição, pendente. A lista vem inteira, sem filtro.
 *
 * @param cartoes        gasto real por cartão, calculado das transações sincronizadas (módulo
 *                        {@code ingestao}) — inalterado, só leitura
 * @param cartoesManuais cartões cadastrados à mão com a fatura declarada por Felipe (módulo
 *                        {@code cartao}) — stopgap até o spike do endpoint {@code bills} da
 *                        Pluggy (ver ROADMAP); não é somado nem reconciliado com {@code cartoes}
 *                        aqui, é o front quem decide como exibir os dois lado a lado
 */
public record EuDoPresenteResponse(
        SaldoDeSobrevivenciaResponse diagnostico,
        List<ResumoDeCategoriaResponse> resumoTriagem,
        List<RecorrenciaResponse> vampiros,
        ResumoCartoesResponse cartoes,
        List<CartaoResponse> cartoesManuais) {
}

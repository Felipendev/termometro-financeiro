package br.com.felipe.termometro.classificacao.application.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * O usuário corrigindo a classificação de uma transação (RN-12).
 *
 * @param aplicarAoGrupo cria uma regra de aprendizado e reclassifica <b>todas</b> as transações do
 *                       mesmo estabelecimento, inclusive as do passado. É o que faz classificar uma
 *                       resolver quarenta — e é por isso que a resposta diz quantas foram afetadas
 *                       antes de o usuário confiar no número.
 */
public record ClassificarTransacaoRequest(
        @NotBlank String categoria,
        @NotBlank String grupo,
        @NotBlank String natureza,
        boolean aplicarAoGrupo) {
}

package br.com.felipe.termometro.classificacao.application.api.response;

/**
 * @param transacoesAfetadas quantas foram reclassificadas — 1 se só esta, N se aplicou ao grupo
 * @param regraCriada        se virou regra permanente (RN-12)
 */
public record ResultadoDaCorrecaoResponse(
        String categoria,
        boolean contaNaVerbaDiaria,
        int transacoesAfetadas,
        boolean regraCriada,
        String mensagem) {
}

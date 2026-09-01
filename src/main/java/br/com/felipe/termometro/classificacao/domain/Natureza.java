package br.com.felipe.termometro.classificacao.domain;

/**
 * O que o gasto é, do ponto de vista do orçamento.
 *
 * <p>É esta enum que separa o que entra na verba diária do que não entra — a distinção mais
 * importante do sistema inteiro.
 */
public enum Natureza {

    /** Valor conhecido e recorrente: aluguel, imposto, contador, internet, assinatura. */
    FIXO,

    /** Decisão do dia: mercado, comer fora, transporte, lazer. É isto que a verba governa. */
    VARIAVEL,

    /**
     * Movimentação que não é consumo: pagamento de fatura, transferência entre contas próprias,
     * amortização de dívida, aporte. Contar como gasto seria contar duas vezes (RN-03).
     */
    NAO_E_GASTO;

    public boolean entraNaVerbaDiaria() {
        return this == VARIAVEL;
    }
}

package br.com.felipe.termometro.triagem.domain;

/**
 * A cor da triagem (RN-05): o que cada transação é, do ponto de vista de "dava para não gastar".
 */
public enum Etiqueta {

    /** Indispensável: fixo, ou a parte do gasto variável dentro do piso humano. */
    AZUL,

    /** Legítima, mas reduzível: a parte do gasto variável que passa do piso. */
    AMARELA,

    /**
     * Não era necessidade naquele momento. Nunca atribuída pelo algoritmo automático — só existe
     * por promoção manual do usuário, sempre a partir de uma transação hoje {@link #AMARELA}.
     */
    VERMELHA,

    /** Não é gasto: amortização de dívida, aporte, investimento. Nunca entra em corte. */
    VERDE,

    /** Categoria variável sem piso humano definido — o algoritmo não tem base para decidir sozinho. */
    NAO_TRIADA;

    public boolean podeSerPromovidaParaVermelha() {
        return this == AMARELA;
    }
}

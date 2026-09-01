package br.com.felipe.termometro.projecao.domain;

/**
 * RN-09 — resultado geral da simulação sobre o horizonte pedido.
 */
public enum StatusProjecao {

    /** Quita dentro do horizonte sem nenhum mês com {@code disponivel <= 0}. */
    VIAVEL,

    /** Quita dentro do horizonte, mas passa por ao menos um mês apertado (edge case 12). */
    VIAVEL_COM_APERTO,

    /** Não quita dentro do horizonte — vem acompanhado de {@code rendaExtraMinimaSugerida}. */
    INVIAVEL
}

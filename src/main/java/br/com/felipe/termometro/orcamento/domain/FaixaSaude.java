package br.com.felipe.termometro.orcamento.domain;

import java.math.BigDecimal;

/**
 * Semáforo do dia (RN-19), medido pela verba de hoje contra a verba base do mês.
 *
 * <p>As faixas são as mesmas da RN-14, aplicadas ao dia. O nome importa: o usuário lê isto todo
 * dia de manhã, e "PESSIMO" precisa querer dizer "o mês estourou", não "você é um fracasso".
 */
public enum FaixaSaude {
    IDEAL("no ritmo, com folga"),
    SEGUINDO_BEM("no ritmo"),
    RUIM("apertado — dá para recuperar no mês"),
    PESSIMO("estourando — hoje é dia de segurar");

    private static final BigDecimal LIMITE_SEGUINDO_BEM = new BigDecimal("0.85");
    private static final BigDecimal LIMITE_RUIM = new BigDecimal("0.60");

    private final String leitura;

    FaixaSaude(String leitura) {
        this.leitura = leitura;
    }

    public String leitura() {
        return leitura;
    }

    /**
     * @param proporcao verba de hoje dividida pela verba base
     */
    public static FaixaSaude de(BigDecimal proporcao) {
        if (proporcao.compareTo(BigDecimal.ONE) >= 0) {
            return IDEAL;
        }
        if (proporcao.compareTo(LIMITE_SEGUINDO_BEM) >= 0) {
            return SEGUINDO_BEM;
        }
        if (proporcao.compareTo(LIMITE_RUIM) >= 0) {
            return RUIM;
        }
        return PESSIMO;
    }
}

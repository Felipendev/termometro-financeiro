package br.com.felipe.termometro.classificacao.domain;

import java.math.BigDecimal;

/**
 * Como a regra casa com a transação, da mais precisa para a menos.
 *
 * <p>O multiplicador de confiança reflete quanto o casamento realmente prova: CNPJ identifica a
 * empresa sem ambiguidade; um trecho de descrição pode casar por acidente.
 */
public enum TipoDeRegra {

    CNPJ(new BigDecimal("1.00")),
    ESTABELECIMENTO(new BigDecimal("0.95")),
    DESCRICAO(new BigDecimal("0.85")),
    /** Dica que o próprio banco mandou — o Itaú manda desde a fatura de agosto. */
    CATEGORIA_DO_BANCO(new BigDecimal("0.80"));

    private final BigDecimal multiplicador;

    TipoDeRegra(BigDecimal multiplicador) {
        this.multiplicador = multiplicador;
    }

    public BigDecimal multiplicador() {
        return multiplicador;
    }
}

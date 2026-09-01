package br.com.felipe.termometro.ingestao.domain;

/**
 * Tipo da conta na instituição.
 *
 * <p>A distinção não é cosmética: <b>é ela que define o sinal do valor</b>. Conta corrente já vem
 * com valor assinado; cartão de crédito vem com compra positiva e precisa ser invertido (RN-01).
 */
public enum TipoDeConta {
    CORRENTE,
    POUPANCA,
    CARTAO_CREDITO;

    public boolean ehCartaoDeCredito() {
        return this == CARTAO_CREDITO;
    }
}

package br.com.felipe.termometro.ingestao.domain;

/**
 * Seções em que uma fatura divide seus lançamentos.
 *
 * <p>A distinção que importa é entre o que <b>compõe o total desta fatura</b> e o que não compõe.
 * A fatura do Itaú lista, no fim, as parcelas que só vão vencer nos próximos meses; somá-las ao
 * gasto do mês infla a despesa com dinheiro que ainda nem foi cobrado. Foi exatamente esse o erro
 * que fez a primeira leitura da fatura de julho dar R$ 4.465 contra os R$ 4.091,57 impressos.
 */
public enum SecaoFatura {
    /** Compras e saques no cartão. */
    CARTAO(true),
    /** Compras em moeda estrangeira, já convertidas. */
    INTERNACIONAL(true),
    /** Pix e transferências feitas no crédito, boletos, saques. */
    PRODUTOS_SERVICOS(true),
    /** Pagamento da fatura anterior — crédito, não despesa. */
    PAGAMENTO(false),
    /** Parcelas de compras já feitas que só vencem nas próximas faturas (RN-04). */
    FUTURO(false),
    /** Simulações, ofertas de parcelamento, textos legais. */
    IGNORAR(false),
    /**
     * Movimento comum de conta corrente ou poupança, sincronizado via Open Finance — não existe
     * "seção de fatura" para esse tipo de conta, mas o lançamento é cashflow real (compõe total)
     * até que a RN-03 identifique que ele é, na verdade, pagamento de fatura, transferência entre
     * contas próprias ou estorno, e o marque {@code ignorada} na entidade.
     */
    MOVIMENTO_CONTA(true);

    private final boolean compoeTotal;

    SecaoFatura(boolean compoeTotal) {
        this.compoeTotal = compoeTotal;
    }

    /** Se os lançamentos desta seção somam no total desta fatura. */
    public boolean compoeTotal() {
        return compoeTotal;
    }
}

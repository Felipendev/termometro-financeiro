package br.com.felipe.termometro.ingestao.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LeitoresPdfTest {

    @Test
    void lePicPayESeparaPagamentoDeCompraInternacional() {
        var resultado = new LeitorPicPayPdf().lerTexto("""
                10/08/2026 Vencimento:
                Total da sua fatura
                R$ 23,65
                Transações Nacionais
                Data Estabelecimento Valor (R$)
                03/08 MERCADO 10,00
                04/08 PAGAMENTO DE FATURA -5,00
                Subtotal dos lançamentos 5,00
                Transações Internacionais
                Data Estabelecimento US$ R$
                05/08
                SERVICO EXTERIOR
                1,60 13,65
                Subtotal dos lançamentos 13,65
                """);

        assertThat(resultado.confiavel()).isTrue();
        assertThat(resultado.totalDeDespesas().toString()).isEqualTo("R$ 23,65");
        assertThat(resultado.transacoes()).hasSize(3);
        assertThat(resultado.transacoes().get(1).ehDespesa()).isFalse();
    }

    @Test
    void leItauSemSomarParcelaFuturaAoTotalAtual() {
        var resultado = new LeitorItauPdf().lerTexto("""
                Vencimento: 10/08/2026
                Total desta fatura 110,00
                Lançamentos: compras e saques
                02/08 RESTAURANTE 50,00
                Lançamentos internacionais
                03/08 SERVICO EXTERIOR 20,00
                Repasse de IOF em R$ 1,00
                Lançamentos: produtos e serviços
                04/08 PIX NO CREDITO 39,00
                Compras parceladas - próximas faturas
                05/08 LOJA 02/04 40,00
                Limites de crédito Valor em R$
                """);

        assertThat(resultado.confiavel()).isTrue();
        assertThat(resultado.totalDeDespesas().toString()).isEqualTo("R$ 110,00");
        assertThat(resultado.compromissosFuturos()).hasSize(1);
    }
}

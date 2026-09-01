package br.com.felipe.termometro.ingestao.infra.nubank;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Dinheiro;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LeitorNubankCsv")
class LeitorNubankCsvTest {

    private ResultadoDaLeitura resultado;

    private static InputStream exemplo() {
        return LeitorNubankCsvTest.class.getResourceAsStream("/faturas/nubank-exemplo.csv");
    }

    @BeforeEach
    void lerArquivo() throws IOException {
        try (InputStream conteudo = exemplo()) {
            resultado = new LeitorNubankCsv().ler(conteudo);
        }
    }

    @Test
    @DisplayName("lê todas as linhas do arquivo")
    void leTodasAsLinhas() {
        assertThat(resultado.transacoes()).hasSize(10);
        assertThat(resultado.avisos()).isEmpty();
    }

    @Test
    @DisplayName("RN-01: inverte o sinal — no Nubank despesa vem positiva")
    void inverteOSinal() {
        TransacaoBruta amazon = porDescricao("Amazon BR IV - NuPay");
        assertThat(amazon.valor())
                .as("R$ 31,90 de gasto tem que virar -31,90 no domínio")
                .isEqualTo(Dinheiro.de("-31.90"));
        assertThat(amazon.ehDespesa()).isTrue();
    }

    @Test
    @DisplayName("pagamento recebido vira crédito positivo e não compõe o total")
    void pagamentoRecebido() {
        TransacaoBruta pagamento = porDescricao("Pagamento recebido");
        assertThat(pagamento.valor()).isEqualTo(Dinheiro.de("2625.03"));
        assertThat(pagamento.secao()).isEqualTo(SecaoFatura.PAGAMENTO);
        assertThat(pagamento.compoeTotalDaFatura()).isFalse();
    }

    @Test
    @DisplayName("'Pix no Crédito - Nu Pagamentos SA' é gasto, não pagamento de fatura")
    void naoConfundePixComPagamento() {
        TransacaoBruta pix = porDescricao("Pix no Crédito - Nu Pagamentos SA");
        assertThat(pix.secao())
                .as("casar 'PAGAMENTO' por substring apagaria R$ 2.436,11 de gasto real")
                .isEqualTo(SecaoFatura.CARTAO);
        assertThat(pix.ehDespesa()).isTrue();
    }

    @Test
    @DisplayName("valor com espaço depois do sinal é aceito")
    void espacoDepoisDoSinal() {
        assertThat(porDescricao("Pagamento recebido").valor()).isEqualTo(Dinheiro.de("2625.03"));
    }

    @Test
    @DisplayName("estorno mantém o sinal oposto ao da compra")
    void estorno() {
        assertThat(resultado.transacoes().stream()
                .filter(t -> t.descricao().equals("BMB*Claro - NuPay"))
                .map(TransacaoBruta::valor)
                .toList())
                .containsExactly(Dinheiro.de("-20.00"), Dinheiro.de("20.00"));
    }

    @Test
    @DisplayName("extrai a parcela do título")
    void extraiParcela() {
        assertThat(porDescricao("Orange Shopping - Parcela 9/10").parcelaOpcional())
                .hasValueSatisfying(p -> {
                    assertThat(p.numero()).isEqualTo(9);
                    assertThat(p.total()).isEqualTo(10);
                    assertThat(p.restantes()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("o CSV não traz hora, então o período do dia não é analisável (RN-12)")
    void semHoraConfiavel() {
        assertThat(resultado.transacoes()).allSatisfy(t -> assertThat(t.horaConfiavel()).isFalse());
    }

    @Test
    @DisplayName("total de despesas soma só o que compõe a fatura")
    void totalDeDespesas() {
        // 31,90 + 9,03 + 2436,11 + 14,98 + 14,98 + 14,96 + 198,80 + 20,00 - 20,00
        assertThat(resultado.totalDeDespesas()).isEqualTo(Dinheiro.de("2720.76"));
    }

    @Test
    @DisplayName("reconcilia contra o total impresso na fatura")
    void reconciliacao() throws IOException {
        try (InputStream conteudo = exemplo()) {
            ResultadoDaLeitura conferido = new LeitorNubankCsv(Dinheiro.de("2720.76")).ler(conteudo);
            assertThat(conferido.conferencia()).hasValueSatisfying(r -> assertThat(r.fecha()).isTrue());
            assertThat(conferido.confiavel()).isTrue();
        }
    }

    @Test
    @DisplayName("divergência com o total impresso vira aviso, não silêncio")
    void reconciliacaoQueNaoFecha() throws IOException {
        try (InputStream conteudo = exemplo()) {
            ResultadoDaLeitura conferido = new LeitorNubankCsv(Dinheiro.de("3000.00")).ler(conteudo);
            assertThat(conferido.confiavel()).isFalse();
            assertThat(conferido.avisos()).anySatisfy(
                    aviso -> assertThat(aviso).contains("NÃO fecha"));
        }
    }

    private TransacaoBruta porDescricao(String descricao) {
        return resultado.transacoes().stream()
                .filter(t -> t.descricao().equals(descricao))
                .findFirst()
                .orElseThrow(() -> new AssertionError("não achei: " + descricao));
    }
}

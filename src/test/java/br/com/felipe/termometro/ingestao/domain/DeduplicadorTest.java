package br.com.felipe.termometro.ingestao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Deduplicador")
class DeduplicadorTest {

    private static final String CONTA = "nubank-1234";
    private static final LocalDate DIA = LocalDate.of(2026, 8, 6);

    private static TransacaoBruta lancamento(String descricao, String valor, Origem origem) {
        return new TransacaoBruta(DIA, null, descricao, descricao, Dinheiro.de(valor),
                null, null, SecaoFatura.CARTAO, null, origem, 0);
    }

    @Test
    @DisplayName("RN-02: quatro cobranças idênticas no mesmo dia são quatro cobranças")
    void naoColapsaCobrancasReais() {
        List<TransacaoBruta> lote = List.of(
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Smartblue Jp", "-14.98", Origem.CSV));

        List<TransacaoBruta> resultado = new Deduplicador(CONTA).absorver(lote).transacoes();

        assertThat(resultado)
                .as("sem o ordinal o sistema apagaria três despesas reais")
                .hasSize(4);
        assertThat(resultado).extracting(TransacaoBruta::ordinal).containsExactly(0, 1, 2, 3);
        assertThat(Dinheiro.somaDe(resultado.stream().map(TransacaoBruta::valor).toList()))
                .isEqualTo(Dinheiro.de("-59.92"));
    }

    @Test
    @DisplayName("reimportar o mesmo arquivo não cria nada")
    void reimportacaoEhIdempotente() {
        List<TransacaoBruta> lote = List.of(
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Supermercado Arruda", "-64.25", Origem.CSV));

        Deduplicador dedup = new Deduplicador(CONTA).absorver(lote).absorver(lote);

        assertThat(dedup.transacoes()).hasSize(3);
        assertThat(dedup.duplicadasDescartadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("OFX substitui o mesmo lançamento vindo de CSV")
    void origemMaisConfiavelVence() {
        Deduplicador dedup = new Deduplicador(CONTA)
                .absorver(List.of(lancamento("Supermercado Arruda", "-64.25", Origem.CSV)))
                .absorver(List.of(lancamento("Supermercado Arruda", "-64.25", Origem.OFX)));

        assertThat(dedup.transacoes()).hasSize(1);
        assertThat(dedup.transacoes().get(0).origem()).isEqualTo(Origem.OFX);
        assertThat(dedup.substituidasPorOrigemMelhor()).isEqualTo(1);
        assertThat(dedup.avisos()).hasSize(1);
    }

    @Test
    @DisplayName("PDF não sobrescreve o que já veio por OFX")
    void origemMenosConfiavelNaoVence() {
        Deduplicador dedup = new Deduplicador(CONTA)
                .absorver(List.of(lancamento("Supermercado Arruda", "-64.25", Origem.OFX)))
                .absorver(List.of(lancamento("Supermercado Arruda", "-64.25", Origem.PDF)));

        assertThat(dedup.transacoes().get(0).origem()).isEqualTo(Origem.OFX);
        assertThat(dedup.duplicadasDescartadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("a ordem de numeração é estável entre importações do mesmo arquivo")
    void numeracaoEstavel() {
        List<TransacaoBruta> lote = List.of(
                lancamento("Uber * Pending", "-5.92", Origem.CSV),
                lancamento("Supermercado Arruda", "-64.25", Origem.CSV),
                lancamento("Uber * Pending", "-5.92", Origem.CSV));

        assertThat(Deduplicador.numerar(lote)).extracting(TransacaoBruta::ordinal)
                .containsExactly(0, 0, 1);
        assertThat(Deduplicador.numerar(lote)).isEqualTo(Deduplicador.numerar(lote));
    }

    @Test
    @DisplayName("valores diferentes no mesmo dia não competem por ordinal")
    void valoresDiferentes() {
        List<TransacaoBruta> lote = List.of(
                lancamento("Smartblue Jp", "-14.98", Origem.CSV),
                lancamento("Smartblue Jp", "-14.96", Origem.CSV));

        assertThat(Deduplicador.numerar(lote)).extracting(TransacaoBruta::ordinal)
                .containsExactly(0, 0);
    }
}

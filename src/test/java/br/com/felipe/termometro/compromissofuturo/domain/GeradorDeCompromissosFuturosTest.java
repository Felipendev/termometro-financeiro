package br.com.felipe.termometro.compromissofuturo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeradorDeCompromissosFuturos")
class GeradorDeCompromissosFuturosTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Test
    @DisplayName("parcela 3/10 gera as 7 parcelas restantes, mês a mês, com o mesmo valor")
    void geraAsParcelasRestantes() {
        LancamentoParceladoAncora ancora = ancora("CONTA-1", "LOJA X", SETEMBRO, "-150.00", 3, 10);

        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of(ancora));

        assertThat(resultado.gerados()).hasSize(7);
        assertThat(resultado.gerados()).extracting(CompromissoFuturo::parcelaNumero)
                .containsExactlyInAnyOrder(4, 5, 6, 7, 8, 9, 10);
        assertThat(resultado.gerados()).allMatch(c -> c.valor().equals(Dinheiro.de("150.00")));
        assertThat(resultado.gerados()).allMatch(c -> c.parcelaTotal() == 10);
        assertThat(resultado.gerados()).allMatch(c -> c.identificadorConta().equals("CONTA-1"));
        // parcela 4 cai em outubro/2026, parcela 10 em abril/2027
        assertThat(resultado.gerados().stream().filter(c -> c.parcelaNumero() == 4).findFirst().orElseThrow()
                .competencia()).isEqualTo(Competencia.de(2026, 10));
        assertThat(resultado.gerados().stream().filter(c -> c.parcelaNumero() == 10).findFirst().orElseThrow()
                .competencia()).isEqualTo(Competencia.de(2027, 4));
        assertThat(resultado.seriesProcessadas()).containsExactly(ancora.chave());
    }

    @Test
    @DisplayName("âncora na última parcela não gera nada, mas a série ainda é processada")
    void ultimaParcelaNaoGeraNada() {
        LancamentoParceladoAncora ancora = ancora("CONTA-1", "LOJA X", SETEMBRO, "-150.00", 9, 9);

        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of(ancora));

        assertThat(resultado.gerados()).isEmpty();
        assertThat(resultado.seriesProcessadas()).containsExactly(ancora.chave());
    }

    @Test
    @DisplayName("entre duas visões da mesma série, só a de maior parcela vira âncora")
    void soAParcelaMaisRecenteViraAncora() {
        LancamentoParceladoAncora antiga = ancora("CONTA-1", "LOJA X", SETEMBRO.menos(1), "-150.00", 2, 10);
        LancamentoParceladoAncora recente = ancora("CONTA-1", "LOJA X", SETEMBRO, "-150.00", 3, 10);

        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of(antiga, recente));

        assertThat(resultado.gerados()).hasSize(7); // 4..10, não 3..10
        assertThat(resultado.gerados()).extracting(CompromissoFuturo::parcelaNumero)
                .doesNotContain(3);
        assertThat(resultado.seriesProcessadas()).hasSize(1);
    }

    @Test
    @DisplayName("contas diferentes com a mesma descrição são séries diferentes")
    void contasDiferentesSaoSeriesDiferentes() {
        LancamentoParceladoAncora contaA = ancora("CONTA-A", "LOJA X", SETEMBRO, "-100.00", 1, 3);
        LancamentoParceladoAncora contaB = ancora("CONTA-B", "LOJA X", SETEMBRO, "-100.00", 1, 3);

        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of(contaA, contaB));

        assertThat(resultado.seriesProcessadas()).hasSize(2);
        assertThat(resultado.gerados()).hasSize(4); // 2 parcelas restantes cada
        assertThat(resultado.gerados()).extracting(CompromissoFuturo::identificadorConta)
                .containsExactlyInAnyOrder("CONTA-A", "CONTA-A", "CONTA-B", "CONTA-B");
    }

    @Test
    @DisplayName("estabelecimentos diferentes na mesma conta são séries diferentes, mesmo com o mesmo total")
    void estabelecimentosDiferentesSaoSeriesDiferentes() {
        LancamentoParceladoAncora lojaX = ancora("CONTA-1", "LOJA X", SETEMBRO, "-100.00", 1, 3);
        LancamentoParceladoAncora lojaY = ancora("CONTA-1", "LOJA Y", SETEMBRO, "-200.00", 1, 3);

        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of(lojaX, lojaY));

        assertThat(resultado.seriesProcessadas()).hasSize(2);
        assertThat(resultado.gerados()).hasSize(4);
    }

    @Test
    @DisplayName("sem lançamentos, nenhuma série e nenhum gerado")
    void semLancamentos() {
        ResultadoDaGeracao resultado = GeradorDeCompromissosFuturos.gera(List.of());

        assertThat(resultado.gerados()).isEmpty();
        assertThat(resultado.seriesProcessadas()).isEmpty();
    }

    private LancamentoParceladoAncora ancora(String conta, String descricaoNormalizada, Competencia competencia,
            String valor, int numero, int total) {
        return new LancamentoParceladoAncora(conta, descricaoNormalizada, descricaoNormalizada + " " + numero + "/"
                + total, "COMPRAS", competencia, Dinheiro.de(valor), numero, total);
    }
}

package br.com.felipe.termometro.planilha.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import org.junit.jupiter.api.Test;

/** RN-18 — os 3 cenários Gherkin de `ESPEC-planilha-viva.md`. */
class ClassificadorDeUsoDeCreditoTest {

    @Test
    void pixNoCreditoComCaixaDoDiaNegativoEDeficitDisfarcado() {
        UsoDeCredito classificacao = ClassificadorDeUsoDeCredito.classifica(
                "PIX Contador", Dinheiro.de("-30"));

        assertThat(classificacao).isEqualTo(UsoDeCredito.DEFICIT_DISFARCADO);
    }

    @Test
    void compraParceladaSemPixComCaixaPositivoEFerramenta() {
        UsoDeCredito classificacao = ClassificadorDeUsoDeCredito.classifica(
                "NOHA SHOES - PARC 01/04", Dinheiro.de("120"));

        assertThat(classificacao).isEqualTo(UsoDeCredito.FERRAMENTA);
    }

    @Test
    void soUmDosDoisSinaisEAtencao() {
        UsoDeCredito classificacao = ClassificadorDeUsoDeCredito.classifica(
                "PIX Loja X", Dinheiro.de("200"));

        assertThat(classificacao).isEqualTo(UsoDeCredito.ATENCAO);
    }

    @Test
    void caixaNegativoSemPixTambemEAtencao() {
        UsoDeCredito classificacao = ClassificadorDeUsoDeCredito.classifica(
                "SUPERMERCADO ARRUDA", Dinheiro.de("-5"));

        assertThat(classificacao).isEqualTo(UsoDeCredito.ATENCAO);
    }
}

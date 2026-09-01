package br.com.felipe.termometro.planilha.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import org.junit.jupiter.api.Test;

class FaixaDeSaldoTest {

    @Test
    void saldoNegativoEVermelho() {
        assertThat(FaixaDeSaldo.de(Dinheiro.de("-1"))).isEqualTo(FaixaDeSaldo.VERMELHO);
    }

    @Test
    void saldoAltoEVerde() {
        assertThat(FaixaDeSaldo.de(Dinheiro.de("10000"))).isEqualTo(FaixaDeSaldo.VERDE);
    }

    @Test
    void saldoNoMeioDaFaixaEAmarelo() {
        assertThat(FaixaDeSaldo.de(Dinheiro.de("2000"))).isEqualTo(FaixaDeSaldo.AMARELO);
    }
}

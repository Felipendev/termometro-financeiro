package br.com.felipe.termometro.rollupanual.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import org.junit.jupiter.api.Test;

class CalculadoraDeTaxaDeEconomiaTest {

    @Test
    void economiaPositivaDaTaxaPositiva() {
        assertThat(CalculadoraDeTaxaDeEconomia.calcula(Dinheiro.de("10000"), Dinheiro.de("7500")))
                .isEqualTo(Percentual.deFracao("0.25"));
    }

    @Test
    void semEntradaDevolveZeroEmVezDeQuebrar() {
        assertThat(CalculadoraDeTaxaDeEconomia.calcula(Dinheiro.ZERO, Dinheiro.de("100")))
                .isEqualTo(Percentual.deFracao("0"));
    }

    @Test
    void saidaMaiorQueEntradaDaTaxaNegativa() {
        assertThat(CalculadoraDeTaxaDeEconomia.calcula(Dinheiro.de("1000"), Dinheiro.de("1200")))
                .isEqualTo(Percentual.deFracao("-0.2"));
    }
}

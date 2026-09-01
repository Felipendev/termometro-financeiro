package br.com.felipe.termometro.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Percentual")
class PercentualTest {

    @Test
    @DisplayName("fração e pontos descrevem o mesmo percentual")
    void fracaoEPontos() {
        assertThat(Percentual.dePontos("25")).isEqualTo(Percentual.deFracao("0.25"));
        assertThat(Percentual.dePontos("25").emPontos()).isEqualByComparingTo("25");
    }

    @ParameterizedTest(name = "{0} de {1} = {2}")
    @DisplayName("percentual entre dois valores")
    @CsvSource({"900, 8200, 10.9756", "6300, 14000, 45.0000", "6300, 10000, 63.0000", "2600, 10000, 26.0000"})
    void percentualEntreValores(String parte, String total, String pontosEsperados) {
        assertThat(Percentual.deValor(Dinheiro.de(parte), Dinheiro.de(total)).emPontos())
                .isEqualByComparingTo(pontosEsperados);
    }

    @Test
    @DisplayName("percentual sobre total zero é indefinido, não zero")
    void totalZero() {
        assertThatExceptionOfType(ArithmeticException.class)
                .isThrownBy(() -> Percentual.deValor(Dinheiro.de(10), Dinheiro.ZERO))
                .withMessageContaining("indefinido");
    }

    @Nested
    @DisplayName("regras de negócio que dependem de percentual")
    class RegrasDeNegocio {

        @Test
        @DisplayName("RN-16: taxa máxima de economia do cenário de aceite é 10,98%")
        void taxaMaximaDeViabilidade() {
            Dinheiro renda = Dinheiro.de("8200");
            Dinheiro custoMinimo = Dinheiro.de("5400").somar(Dinheiro.de("1900"));
            Dinheiro economiaMaxima = renda.subtrair(custoMinimo);

            assertThat(economiaMaxima).isEqualTo(Dinheiro.de("900"));
            assertThat(economiaMaxima.sobre(renda).formatado(2)).isEqualTo("10,98%");
            assertThat(economiaMaxima.sobre(renda).menorQue(Percentual.dePontos("25")))
                    .as("abaixo da meta: veredito VIAVEL_PARCIALMENTE")
                    .isTrue();
        }

        @Test
        @DisplayName("RN-16: alvo de redução do custo fixo é 1.150,00")
        void alvoDeReducaoDoFixo() {
            Dinheiro renda = Dinheiro.de("8200");
            Dinheiro alvo = Percentual.dePontos("25").aplicarSobre(renda).subtrair(Dinheiro.de("900"));
            assertThat(alvo).isEqualTo(Dinheiro.de("1150"));
        }

        @Test
        @DisplayName("RN-16.1: a queda de renda de 14k para 10k é de 28,6%")
        void quedaEstruturalDeRenda() {
            Percentual queda = Percentual.CEM.subtrair(
                    Dinheiro.de(10_000).sobre(Dinheiro.de(14_000)));
            assertThat(queda.formatado()).isEqualTo("28,6%");
            assertThat(queda.formatado(2)).isEqualTo("28,57%");
        }

        @Test
        @DisplayName("RN-16.1: o mesmo custo fixo passa de 45% para 63% da renda")
        void pesoDoCustoFixoAntesEDepois() {
            Dinheiro fixo = Dinheiro.de("6300");
            assertThat(fixo.sobre(Dinheiro.de(14_000)).formatado()).isEqualTo("45,0%");
            assertThat(fixo.sobre(Dinheiro.de(10_000)).formatado()).isEqualTo("63,0%");
        }

        @Test
        @DisplayName("RN-14: taxa de economia de 26% cai na faixa IDEAL")
        void faixaGlobalIdeal() {
            Dinheiro renda = Dinheiro.de(10_000);
            Percentual taxa = renda.subtrair(Dinheiro.de("7400")).sobre(renda);
            assertThat(taxa.formatado()).isEqualTo("26,0%");
            assertThat(taxa.maiorOuIgualA(Percentual.dePontos("25"))).isTrue();
        }
    }

    @Test
    @DisplayName("formatação em pt-BR")
    void formatacao() {
        assertThat(Percentual.dePontos("25")).hasToString("25,0%");
        assertThat(Percentual.deFracao("0.285714").formatado(2)).isEqualTo("28,57%");
        assertThat(Percentual.deFracao("-0.5").formatado()).isEqualTo("-50,0%");
        assertThat(Percentual.ZERO.formatado(0)).isEqualTo("0%");
    }
}

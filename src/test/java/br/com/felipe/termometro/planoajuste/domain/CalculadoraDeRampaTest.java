package br.com.felipe.termometro.planoajuste.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraDeRampaTest {

    private static final BigDecimal FATOR_35 = new BigDecimal("0.35");

    @Nested
    @DisplayName("cenários Gherkin da RN-15")
    class CenariosGherkin {

        @Test
        @DisplayName("rampa respeita o piso humano e o limite de corte: 1.240 -> 320, alonga 3->4 meses")
        void rampaRespeitaPisoELimiteDeCorte() {
            ResultadoDaRampa resultado =
                    CalculadoraDeRampa.calcular(Dinheiro.de("1240.00"), Dinheiro.de("320.00"), FATOR_35, 3);

            assertThat(resultado.mesesEfetivos()).isEqualTo(4);
            assertThat(resultado.alongada()).isTrue();

            List<AlvoMensal> alvos = resultado.alvosMensais();
            assertThat(alvos).hasSize(4);
            assertThat(alvos.get(0).alvo().valor()).isEqualByComparingTo("883.80");
            assertThat(alvos.get(1).alvo().valor()).isEqualByComparingTo("629.92");
            assertThat(alvos.get(2).alvo().valor()).isEqualByComparingTo("448.97");
            assertThat(alvos.get(3).alvo().valor()).isEqualByComparingTo("320.00");

            for (AlvoMensal alvo : alvos) {
                assertThat(alvo.reducaoPercentual().fracao().doubleValue())
                        .isCloseTo(0.287, within(0.001));
            }
        }

        @Test
        @DisplayName("corte muito profundo alonga a rampa até caber no limite: 1.000 -> 100, pedido 2 meses vira 6")
        void corteProfundoAlongaAteOLimite() {
            ResultadoDaRampa resultado =
                    CalculadoraDeRampa.calcular(Dinheiro.de("1000.00"), Dinheiro.de("100.00"), FATOR_35, 2);

            assertThat(resultado.mesesEfetivos()).isEqualTo(6);
            assertThat(resultado.alongada()).isTrue();

            List<AlvoMensal> alvos = resultado.alvosMensais();
            assertThat(alvos).hasSize(6);
            assertThat(alvos.get(5).alvo().valor()).isEqualByComparingTo("100.00");

            for (AlvoMensal alvo : alvos) {
                assertThat(alvo.reducaoPercentual().fracao().doubleValue())
                        .isCloseTo(0.319, within(0.001));
                // nenhuma redução mensal ultrapassa 35%
                assertThat(alvo.reducaoPercentual().fracao().doubleValue()).isLessThanOrEqualTo(0.35);
            }
        }

        @Test
        @DisplayName("rampa curta o suficiente não é alongada: 310 -> 180 em 3 meses")
        void rampaCurtaNaoEAlongada() {
            ResultadoDaRampa resultado =
                    CalculadoraDeRampa.calcular(Dinheiro.de("310.00"), Dinheiro.de("180.00"), FATOR_35, 3);

            assertThat(resultado.mesesEfetivos()).isEqualTo(3);
            assertThat(resultado.alongada()).isFalse();

            List<AlvoMensal> alvos = resultado.alvosMensais();
            assertThat(alvos.get(2).alvo().valor()).isEqualByComparingTo("180.00");
            for (AlvoMensal alvo : alvos) {
                assertThat(alvo.reducaoPercentual().fracao().doubleValue())
                        .isCloseTo(0.166, within(0.001));
            }
        }
    }

    @Nested
    @DisplayName("validação de pré-condições")
    class Validacao {

        @Test
        @DisplayName("atual <= alvo é responsabilidade do motor, não da calculadora")
        void atualMenorOuIgualAlvoLancaExcecao() {
            assertThatThrownBy(() -> CalculadoraDeRampa.calcular(
                    Dinheiro.de("100.00"), Dinheiro.de("100.00"), FATOR_35, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deve ser maior que alvo");
        }

        @Test
        @DisplayName("alvo zero não é suportado — quem filtra é o motor")
        void alvoZeroLancaExcecao() {
            assertThatThrownBy(() -> CalculadoraDeRampa.calcular(
                    Dinheiro.de("100.00"), Dinheiro.ZERO, FATOR_35, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("alvo não pode ser zero");
        }

        @Test
        @DisplayName("mesesSolicitados menor que 1 é inválido")
        void mesesSolicitadosInvalido() {
            assertThatThrownBy(() -> CalculadoraDeRampa.calcular(
                    Dinheiro.de("100.00"), Dinheiro.de("50.00"), FATOR_35, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mesesSolicitados");
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("N_min maior que 12 é limitado a 12 (horizonte máximo)")
        void nuncaExcedeDozeMeses() {
            // corte extremo: 10.000 -> 1,00 com fator de corte baixo força N_min bem alto
            ResultadoDaRampa resultado = CalculadoraDeRampa.calcular(
                    Dinheiro.de("10000.00"), Dinheiro.de("1.00"), new BigDecimal("0.10"), 1);

            assertThat(resultado.mesesEfetivos()).isEqualTo(12);
            assertThat(resultado.alvosMensais()).hasSize(12);
        }

        @Test
        @DisplayName("N solicitado maior que N_min é respeitado (rampa mais suave que o mínimo)")
        void respeitaHorizonteMaiorQueOMinimo() {
            ResultadoDaRampa resultado =
                    CalculadoraDeRampa.calcular(Dinheiro.de("310.00"), Dinheiro.de("180.00"), FATOR_35, 8);

            assertThat(resultado.mesesEfetivos()).isEqualTo(8);
            assertThat(resultado.alongada()).isFalse();
            assertThat(resultado.alvosMensais().get(7).alvo().valor()).isEqualByComparingTo("180.00");
        }
    }
}

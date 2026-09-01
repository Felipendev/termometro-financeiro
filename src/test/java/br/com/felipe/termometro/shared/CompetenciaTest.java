package br.com.felipe.termometro.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Competencia")
class CompetenciaTest {

    private static final Competencia SETEMBRO_26 = Competencia.parse("2026-09");

    private static Clock em(int ano, int mes, int dia) {
        return Clock.fixed(
                LocalDate.of(ano, mes, dia).atStartOfDay(Competencia.FUSO).toInstant(),
                Competencia.FUSO);
    }

    @Nested
    @DisplayName("construção e navegação")
    class Navegacao {

        @Test
        @DisplayName("parse e toString usam o mesmo formato ISO")
        void formatoIso() {
            assertThat(SETEMBRO_26).hasToString("2026-09");
            assertThat(Competencia.parse(SETEMBRO_26.toString())).isEqualTo(SETEMBRO_26);
        }

        @Test
        @DisplayName("próxima e anterior cruzam a virada de ano")
        void viradaDeAno() {
            assertThat(Competencia.de(2026, 12).proxima()).isEqualTo(Competencia.de(2027, 1));
            assertThat(Competencia.de(2026, 1).anterior()).isEqualTo(Competencia.de(2025, 12));
        }

        @Test
        @DisplayName("mesesAte tem sinal")
        void mesesAte() {
            assertThat(Competencia.de(2025, 9).mesesAte(SETEMBRO_26)).isEqualTo(12);
            assertThat(SETEMBRO_26.mesesAte(Competencia.de(2025, 9))).isEqualTo(-12);
            assertThat(SETEMBRO_26.mesesAte(SETEMBRO_26)).isZero();
        }

        @Test
        @DisplayName("o intervalo é fechado nas duas pontas e vazio se invertido")
        void intervalo() {
            assertThat(Competencia.de(2026, 1).ate(Competencia.de(2026, 4)))
                    .containsExactly(Competencia.de(2026, 1), Competencia.de(2026, 2),
                            Competencia.de(2026, 3), Competencia.de(2026, 4));
            assertThat(Competencia.de(2026, 4).ate(Competencia.de(2026, 1))).isEmpty();
            assertThat(SETEMBRO_26.ate(SETEMBRO_26)).containsExactly(SETEMBRO_26);
        }
    }

    @Nested
    @DisplayName("calendário")
    class Calendario {

        @ParameterizedTest(name = "{0}-{1} tem {2} dias")
        @DisplayName("edge case 18: a quantidade de dias nunca é 30 fixo")
        @CsvSource({"2026, 9, 30", "2026, 1, 31", "2026, 2, 28", "2024, 2, 29", "2026, 4, 30"})
        void quantidadeDeDias(int ano, int mes, int esperado) {
            assertThat(Competencia.de(ano, mes).quantidadeDeDias()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("contém apenas datas do próprio mês")
        void contem() {
            assertThat(SETEMBRO_26.contem(LocalDate.of(2026, 9, 1))).isTrue();
            assertThat(SETEMBRO_26.contem(LocalDate.of(2026, 9, 30))).isTrue();
            assertThat(SETEMBRO_26.contem(LocalDate.of(2026, 10, 1))).isFalse();
            assertThat(SETEMBRO_26.contem(LocalDate.of(2026, 8, 31))).isFalse();
        }
    }

    @Nested
    @DisplayName("RN-10: run-rate — mês parcial nunca compara com mês fechado")
    class RunRate {

        private final Clock dia5DeSetembro = em(2026, 9, 5);

        @Test
        @DisplayName("dias decorridos depende da posição no tempo")
        void diasDecorridos() {
            assertThat(SETEMBRO_26.diasDecorridos(dia5DeSetembro))
                    .as("mês corrente: o dia de hoje").isEqualTo(5);
            assertThat(Competencia.de(2026, 8).diasDecorridos(dia5DeSetembro))
                    .as("mês fechado: o mês inteiro").isEqualTo(31);
            assertThat(Competencia.de(2026, 10).diasDecorridos(dia5DeSetembro))
                    .as("mês futuro: nenhum dia").isZero();
        }

        @Test
        @DisplayName("só o mês fechado deixa de ser parcial")
        void parcialidade() {
            assertThat(SETEMBRO_26.ehParcial(dia5DeSetembro)).isTrue();
            assertThat(Competencia.de(2026, 8).ehParcial(dia5DeSetembro)).isFalse();
            assertThat(Competencia.de(2026, 10).ehParcial(dia5DeSetembro)).isTrue();
        }

        @Test
        @DisplayName("critério de aceite: R$ 210,00 em 3 dias projeta R$ 2.100,00")
        void cenarioDeAceite() {
            assertThat(SETEMBRO_26.projetarRunRate(Dinheiro.de("210"), em(2026, 9, 3)))
                    .isEqualTo(Dinheiro.de("2100"));
        }

        @Test
        @DisplayName("mês fechado não extrapola: o fator é 1")
        void mesFechadoNaoExtrapola() {
            assertThat(Competencia.de(2026, 8).fatorRunRate(dia5DeSetembro))
                    .isEqualByComparingTo(BigDecimal.ONE);
            assertThat(Competencia.de(2026, 8).projetarRunRate(Dinheiro.de(1000), dia5DeSetembro))
                    .isEqualTo(Dinheiro.de(1000));
        }

        @Test
        @DisplayName("extrapolar mês futuro falha alto em vez de devolver zero")
        void mesFuturoNaoTemRunRate() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> Competencia.de(2026, 10).fatorRunRate(dia5DeSetembro))
                    .withMessageContaining("futura");
        }

        @Test
        @DisplayName("no último dia do mês o fator é 1")
        void ultimoDiaDoMes() {
            assertThat(SETEMBRO_26.fatorRunRate(em(2026, 9, 30)))
                    .isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("RN-14: ritmo de consumo")
    class Ritmo {

        @Test
        @DisplayName("critério de aceite: consumo 0,50 no dia 5 é ritmo 3,00")
        void consumoBaixoComRitmoAcelerado() {
            Clock dia5 = em(2026, 9, 5);
            Dinheiro meta = Dinheiro.de(800);
            Dinheiro gastoAteHoje = Dinheiro.de(400);

            Percentual consumo = gastoAteHoje.sobre(meta);
            Percentual fracaoDoMes = SETEMBRO_26.fracaoDecorrida(dia5);
            BigDecimal ritmo = consumo.fracao()
                    .divide(fracaoDoMes.fracao(), 2, RoundingMode.HALF_EVEN);

            assertThat(consumo.formatado()).isEqualTo("50,0%");
            assertThat(fracaoDoMes.formatado()).isEqualTo("16,7%");
            assertThat(ritmo).isEqualByComparingTo("3.00");
            assertThat(SETEMBRO_26.projetarRunRate(gastoAteHoje, dia5).sobre(meta).emPontos())
                    .as("consumo projetado de 300% cai na faixa PESSIMO")
                    .isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("num mês fechado a fração decorrida é 100%")
        void mesFechado() {
            assertThat(Competencia.de(2026, 8).fracaoDecorrida(em(2026, 9, 5)))
                    .isEqualTo(Percentual.CEM);
        }
    }
}

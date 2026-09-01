package br.com.felipe.termometro.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Dinheiro")
class DinheiroTest {

    @Nested
    @DisplayName("construção e normalização")
    class Construcao {

        @Test
        @DisplayName("normaliza a escala para 2 casas")
        void normalizaEscala() {
            assertThat(Dinheiro.de("10.5").paraJson()).isEqualTo("10.50");
            assertThat(Dinheiro.de("10").paraJson()).isEqualTo("10.00");
        }

        @Test
        @DisplayName("igualdade ignora a escala de origem")
        void igualdadeIgnoraEscala() {
            assertThat(Dinheiro.de("10.5")).isEqualTo(Dinheiro.de("10.50"));
            assertThat(Dinheiro.de("10.5")).hasSameHashCodeAs(Dinheiro.de("10.500"));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @DisplayName("arredonda com HALF_EVEN")
        @CsvSource({"2.345, 2.34", "2.355, 2.36", "2.365, 2.36", "2.375, 2.38", "-2.345, -2.34"})
        void arredondaHalfEven(String entrada, String esperado) {
            assertThat(Dinheiro.de(entrada).paraJson()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("converte de e para centavos")
        void centavos() {
            assertThat(Dinheiro.de("1234.56").centavos()).isEqualTo(123_456L);
            assertThat(Dinheiro.deCentavos(-8_990).paraJson()).isEqualTo("-89.90");
        }

        @Test
        @DisplayName("rejeita valor nulo")
        void rejeitaNulo() {
            assertThatNullPointerException().isThrownBy(() -> Dinheiro.de((String) null));
            assertThatNullPointerException().isThrownBy(() -> Dinheiro.de((BigDecimal) null));
        }
    }

    @Nested
    @DisplayName("aritmética")
    class Aritmetica {

        @Test
        @DisplayName("soma e subtrai preservando centavos")
        void somaESubtrai() {
            assertThat(Dinheiro.de("0.10").somar(Dinheiro.de("0.20")))
                    .as("0,10 + 0,20 tem que dar exatamente 0,30 — em double daria 0.30000000000000004")
                    .isEqualTo(Dinheiro.de("0.30"));
            assertThat(Dinheiro.de(1000).subtrair(Dinheiro.de(300))).isEqualTo(Dinheiro.de(700));
        }

        @Test
        @DisplayName("RN-08: o déficit do cenário de aceite dá -380,00")
        void deficitDoCenarioDeAceite() {
            Dinheiro saldo = Dinheiro.de("7400")
                    .subtrair(Dinheiro.de("4100"))
                    .subtrair(Dinheiro.de("2300"))
                    .subtrair(Dinheiro.de("1380"));
            assertThat(saldo).isEqualTo(Dinheiro.de("-380"));
            assertThat(saldo.ehNegativo()).isTrue();
        }

        @Test
        @DisplayName("somaDe coleção vazia é zero, não nulo")
        void somaDeVazio() {
            assertThat(Dinheiro.somaDe(List.of())).isEqualTo(Dinheiro.ZERO);
        }

        @Test
        @DisplayName("divisão por zero falha alto")
        void divisaoPorZero() {
            assertThatExceptionOfType(ArithmeticException.class)
                    .isThrownBy(() -> Dinheiro.de(10).dividirPor(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("máximo e mínimo")
        void maximoEMinimo() {
            assertThat(Dinheiro.de(10).maximo(Dinheiro.de(20))).isEqualTo(Dinheiro.de(20));
            assertThat(Dinheiro.de(10).minimo(Dinheiro.de(20))).isEqualTo(Dinheiro.de(10));
        }
    }

    @Nested
    @DisplayName("RN-08: arredondamento para cima em múltiplos")
    class ArredondarParaCima {

        @ParameterizedTest(name = "{0} com múltiplo {1} -> {2}")
        @CsvSource({"380, 50, 400.00", "400, 50, 400.00", "0.01, 50, 50.00", "0, 50, 0.00", "351, 50, 400.00"})
        void arredonda(String valor, String multiplo, String esperado) {
            assertThat(Dinheiro.de(valor).arredondarParaCima(Dinheiro.de(multiplo)).paraJson())
                    .isEqualTo(esperado);
        }

        @Test
        @DisplayName("múltiplo não positivo é erro de programação")
        void multiploInvalido() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Dinheiro.de(10).arredondarParaCima(Dinheiro.ZERO));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Dinheiro.de(10).arredondarParaCima(Dinheiro.de(-50)));
        }
    }

    @Nested
    @DisplayName("rateio em partes iguais")
    class RateioIgual {

        @Test
        @DisplayName("R$ 100,00 em 3 não perde o centavo")
        void centavoNaoEvapora() {
            List<Dinheiro> partes = Dinheiro.de(100).ratear(3);
            assertThat(partes).containsExactly(
                    Dinheiro.de("33.34"), Dinheiro.de("33.33"), Dinheiro.de("33.33"));
            assertThat(Dinheiro.somaDe(partes)).isEqualTo(Dinheiro.de(100));
        }

        @Test
        @DisplayName("valor negativo mantém o sinal e a soma exata")
        void valorNegativo() {
            List<Dinheiro> partes = Dinheiro.de(-100).ratear(3);
            assertThat(partes).allMatch(Dinheiro::ehNegativo);
            assertThat(Dinheiro.somaDe(partes)).isEqualTo(Dinheiro.de(-100));
        }

        @Test
        @DisplayName("divisão exata não gera ajuste")
        void divisaoExata() {
            assertThat(Dinheiro.de(100).ratear(4))
                    .containsOnly(Dinheiro.de(25));
        }

        @Test
        @DisplayName("partes não positivas são erro de programação")
        void partesInvalidas() {
            assertThatIllegalArgumentException().isThrownBy(() -> Dinheiro.de(100).ratear(0));
            assertThatIllegalArgumentException().isThrownBy(() -> Dinheiro.de(100).ratear(-1));
        }
    }

    @Nested
    @DisplayName("rateio proporcional (método do maior resto)")
    class RateioPorPesos {

        @Test
        @DisplayName("pesos iguais equivalem ao rateio em partes iguais")
        void pesosIguais() {
            List<BigDecimal> pesos = List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
            assertThat(Dinheiro.de(100).ratear(pesos)).isEqualTo(Dinheiro.de(100).ratear(3));
        }

        @Test
        @DisplayName("RN-09: amortização proporcional entre duas dívidas")
        void amortizacaoProporcional() {
            List<Dinheiro> alocado = Dinheiro.de(1000)
                    .ratear(List.of(new BigDecimal("8000"), new BigDecimal("2000")));
            assertThat(alocado).containsExactly(Dinheiro.de(800), Dinheiro.de(200));
        }

        @Test
        @DisplayName("peso zero recebe zero e não quebra a soma")
        void pesoZero() {
            List<Dinheiro> alocado = Dinheiro.de(100).ratear(List.of(BigDecimal.ONE, BigDecimal.ZERO));
            assertThat(alocado.get(1)).isEqualTo(Dinheiro.ZERO);
            assertThat(Dinheiro.somaDe(alocado)).isEqualTo(Dinheiro.de(100));
        }

        @Test
        @DisplayName("pesos inválidos são erro de programação")
        void pesosInvalidos() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Dinheiro.de(100).ratear(List.<BigDecimal>of()));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Dinheiro.de(100).ratear(List.of(BigDecimal.ZERO, BigDecimal.ZERO)));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Dinheiro.de(100).ratear(List.of(BigDecimal.ONE, new BigDecimal("-1"))));
        }
    }

    @Nested
    @DisplayName("apresentação")
    class Apresentacao {

        @Test
        @DisplayName("toString em pt-BR com o sinal antes do símbolo")
        void formatoPtBr() {
            assertThat(Dinheiro.de("1234.56")).hasToString("R$ 1.234,56");
            assertThat(Dinheiro.de("-89.90")).hasToString("-R$ 89,90");
            assertThat(Dinheiro.ZERO).hasToString("R$ 0,00");
        }

        @Test
        @DisplayName("paraJson é a forma canônica de serialização")
        void serializacao() {
            assertThat(Dinheiro.de("1234.5").paraJson()).isEqualTo("1234.50");
        }
    }
}

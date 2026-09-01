package br.com.felipe.termometro.projecao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RN-09 — motor de projeção, validado contra os números pré-verificados do Anexo B e o Gherkin
 * da spec: avalanche vs. bola de neve (A 8k@3,5% / B 2k@1,2%, disponível 1.500 → ambas quitam
 * em 8 meses; juros 1.191,48 vs 1.517,22) e a busca binária da renda extra mínima (20k@3,5%
 * a.m., 24 meses → R$ 1.245,46/mês).
 */
class MotorDeProjecaoTest {

    private static final Competencia INICIO = Competencia.de(2026, 9);
    private static final int HORIZONTE_PADRAO = 60;

    private static Function<Competencia, Dinheiro> constante(Dinheiro valor) {
        return m -> valor;
    }

    @Nested
    @DisplayName("Avalanche vs. bola de neve (Anexo B)")
    class AvalancheVsBolaDeNeve {

        private final List<SaldoInicialDeDivida> dividas = List.of(
                new SaldoInicialDeDivida("A", Dinheiro.de("8000.00"), Percentual.dePontos("3.5")),
                new SaldoInicialDeDivida("B", Dinheiro.de("2000.00"), Percentual.dePontos("1.2")));

        private Projecao projetar(Estrategia estrategia) {
            return MotorDeProjecao.projetar(INICIO, HORIZONTE_PADRAO, constante(Dinheiro.de("1500.00")),
                    constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), dividas,
                    estrategia, 0);
        }

        @Test
        @DisplayName("ambas as estratégias quitam em 8 meses")
        void ambasQuitamEm8Meses() {
            assertThat(projetar(Estrategia.AVALANCHE).marcos().mesesAteQuitacao()).isEqualTo(8);
            assertThat(projetar(Estrategia.BOLA_DE_NEVE).marcos().mesesAteQuitacao()).isEqualTo(8);
        }

        @Test
        @DisplayName("juros totais batem exatamente com o Anexo B: 1.191,48 (avalanche) vs 1.517,22 (bola de neve)")
        void jurosTotaisBatemComOAnexoB() {
            Dinheiro jurosAvalanche = projetar(Estrategia.AVALANCHE).marcos().jurosTotaisPagos();
            Dinheiro jurosBolaDeNeve = projetar(Estrategia.BOLA_DE_NEVE).marcos().jurosTotaisPagos();
            assertThat(jurosAvalanche).isEqualTo(Dinheiro.de("1191.48"));
            assertThat(jurosBolaDeNeve).isEqualTo(Dinheiro.de("1517.22"));
            assertThat(jurosAvalanche).isLessThan(jurosBolaDeNeve);
        }

        @Test
        @DisplayName("avalanche amortiza a dívida de maior taxa primeiro")
        void avalancheAmortizaMaiorTaxaPrimeiro() {
            Projecao projecao = projetar(Estrategia.AVALANCHE);
            MesProjetado primeiroMes = projecao.meses().get(0);
            // com disponível 1.500 > total devido de A no primeiro mês (8.000 + juros), a
            // amortização inteira do primeiro mês vai para A — ela ainda não teria começado
            // a cair se a prioridade fosse B.
            assertThat(primeiroMes.amortizacao()).isEqualTo(Dinheiro.de("1500.00"));
        }

        @Test
        @DisplayName("status é VIAVEL — nenhum mês fica apertado")
        void statusEhViavel() {
            assertThat(projetar(Estrategia.AVALANCHE).status()).isEqualTo(StatusProjecao.VIAVEL);
            assertThat(projetar(Estrategia.BOLA_DE_NEVE).status()).isEqualTo(StatusProjecao.VIAVEL);
        }

        @Test
        @DisplayName("saldo de dívida nunca é negativo em nenhum mês, em nenhuma estratégia")
        void saldoNuncaNegativo() {
            for (Estrategia estrategia : Estrategia.values()) {
                Projecao projecao = projetar(estrategia);
                assertThat(projecao.meses()).allSatisfy(mes ->
                        assertThat(mes.saldoDividaFimDoMes().ehNegativo()).isFalse());
            }
        }
    }

    @Nested
    @DisplayName("Cenário inviável (Gherkin)")
    class CenarioInviavel {

        @Test
        @DisplayName("disponível zero e dívida de 20k a 3,5% a.m. → INVIAVEL com renda extra mínima de R$ 1.245,46")
        void inviavelSugereRendaExtraMinima() {
            List<SaldoInicialDeDivida> dividas = List.of(
                    new SaldoInicialDeDivida("única", Dinheiro.de("20000.00"), Percentual.dePontos("3.5")));

            Projecao projecao = MotorDeProjecao.projetar(INICIO, HORIZONTE_PADRAO, constante(Dinheiro.ZERO),
                    constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), dividas,
                    Estrategia.AVALANCHE, 0);

            assertThat(projecao.status()).isEqualTo(StatusProjecao.INVIAVEL);
            assertThat(projecao.rendaExtraMinimaSugerida()).isEqualTo(Dinheiro.de("1245.46"));
            assertThat(projecao.marcos().dataQuitacao()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("sem dívidas, quitação é imediata no primeiro mês")
        void semDividasQuitaImediatamente() {
            Projecao projecao = MotorDeProjecao.projetar(INICIO, 12, constante(Dinheiro.de("1000.00")),
                    constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), List.of(),
                    Estrategia.AVALANCHE, 0);

            assertThat(projecao.status()).isEqualTo(StatusProjecao.VIAVEL);
            assertThat(projecao.marcos().dataQuitacao()).isEqualTo(INICIO);
            assertThat(projecao.marcos().mesesAteQuitacao()).isEqualTo(1);
        }

        @Test
        @DisplayName("disponível negativo todo mês marca apertado e a dívida cresce")
        void disponivelNegativoMarcaApertado() {
            List<SaldoInicialDeDivida> dividas = List.of(
                    new SaldoInicialDeDivida("única", Dinheiro.de("1000.00"), Percentual.dePontos("1.0")));

            Projecao projecao = MotorDeProjecao.projetar(INICIO, 3, constante(Dinheiro.de("500.00")),
                    constante(Dinheiro.ZERO), constante(Dinheiro.de("800.00")), constante(Dinheiro.ZERO),
                    dividas, Estrategia.AVALANCHE, 0);

            assertThat(projecao.meses()).allSatisfy(mes -> assertThat(mes.apertado()).isTrue());
            assertThat(projecao.meses()).allSatisfy(mes -> assertThat(mes.amortizacao()).isEqualTo(Dinheiro.ZERO));
            Dinheiro saldoFinal = projecao.meses().get(2).saldoDividaFimDoMes();
            assertThat(saldoFinal).isGreaterThan(Dinheiro.de("1000.00"));
        }

        @Test
        @DisplayName("proporcional reparte a amortização preservando o total exato")
        void proporcionalPreservaOTotal() {
            List<SaldoInicialDeDivida> dividas = List.of(
                    new SaldoInicialDeDivida("A", Dinheiro.de("3000.00"), Percentual.dePontos("2.0")),
                    new SaldoInicialDeDivida("B", Dinheiro.de("1000.00"), Percentual.dePontos("5.0")));

            Projecao projecao = MotorDeProjecao.projetar(INICIO, 1, constante(Dinheiro.de("800.00")),
                    constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), constante(Dinheiro.ZERO), dividas,
                    Estrategia.PROPORCIONAL, 0);

            MesProjetado primeiroMes = projecao.meses().get(0);
            assertThat(primeiroMes.amortizacao()).isEqualTo(Dinheiro.de("800.00"));
        }

        @Test
        @DisplayName("reserva acumula quando não há dívida e nunca decresce")
        void reservaAcumulaSemDivida() {
            Projecao projecao = MotorDeProjecao.projetar(INICIO, 6, constante(Dinheiro.de("1000.00")),
                    constante(Dinheiro.ZERO), constante(Dinheiro.de("400.00")), constante(Dinheiro.ZERO),
                    List.of(), Estrategia.AVALANCHE, 0);

            List<MesProjetado> meses = projecao.meses();
            for (int i = 1; i < meses.size(); i++) {
                assertThat(meses.get(i).reservaAcumulada()).isGreaterThanOrEqualTo(meses.get(i - 1).reservaAcumulada());
            }
            assertThat(projecao.marcos().primeiroRealGuardado()).isEqualTo(INICIO);
        }
    }
}

package br.com.felipe.termometro.orcamento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CalculadoraDeVerbaDiaria — o Termômetro")
class CalculadoraDeVerbaDiariaTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final VerbaMensal VERBA =
            new VerbaMensal(SETEMBRO, Dinheiro.de(3000), Dinheiro.de(250));
    private final CalculadoraDeVerbaDiaria calculadora = CalculadoraDeVerbaDiaria.padrao();

    private static Clock em(int dia) {
        return Clock.fixed(LocalDate.of(2026, 9, dia).atStartOfDay(Competencia.FUSO).toInstant(),
                Competencia.FUSO);
    }

    private static GastoDoDia gasto(int dia, String valor) {
        return new GastoDoDia(LocalDate.of(2026, 9, dia), Dinheiro.de(valor));
    }

    @Nested
    @DisplayName("RN-20: a provisão fica dentro da verba")
    class Provisao {

        @Test
        @DisplayName("verba de R$ 3.000 com provisão de R$ 250 dá R$ 91,67 por dia")
        void diaADia() {
            assertThat(VERBA.diaADia()).isEqualTo(Dinheiro.de(2750));
            assertThat(VERBA.verbaBase()).isEqualTo(Dinheiro.de("91.67"));
        }

        @Test
        @DisplayName("provisão maior que a verba é erro — ela é uma camada, não um acréscimo")
        void provisaoNaoPodeExcederAVerba() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new VerbaMensal(SETEMBRO, Dinheiro.de(100), Dinheiro.de(200)))
                    .withMessageContaining("dentro da verba");
        }

        @Test
        @DisplayName("eventos que cabem na provisão não mexem na verba diária")
        void eventosDentroDaProvisao() {
            var evento = Evento.previsto(LocalDate.of(2026, 9, 20), "Presente", Dinheiro.de(200));
            var hoje = calculadora.calcular(VERBA, List.of(), List.of(evento), em(1));

            assertThat(hoje.verbaBase()).isEqualTo(Dinheiro.de("91.67"));
            assertThat(hoje.mensagem()).doesNotContain("passaram da provisão");
        }

        @Test
        @DisplayName("evento que estoura a provisão sai do dia a dia — e o aviso vem antes")
        void eventosAcimaDaProvisao() {
            var eventos = List.of(
                    Evento.previsto(LocalDate.of(2026, 9, 12), "Evento", Dinheiro.de(170)),
                    Evento.previsto(LocalDate.of(2026, 9, 5), "Viagem de sábado", Dinheiro.de(110)));

            var hoje = calculadora.calcular(VERBA, List.of(), eventos, em(1));

            assertThat(hoje.reservadoParaEventos()).isEqualTo(Dinheiro.de(280));
            assertThat(hoje.verbaBase())
                    .as("R$ 280 de eventos contra R$ 250 de provisão: R$ 30 saem do dia a dia")
                    .isEqualTo(Dinheiro.de("90.67"));
            assertThat(hoje.mensagem()).contains("passaram da provisão");
        }

        @Test
        @DisplayName("evento nos próximos 3 dias é avisado com o valor já reservado")
        void avisoAntecipado() {
            var eventos = List.of(
                    Evento.previsto(LocalDate.of(2026, 9, 5), "Viagem de sábado", Dinheiro.de(110)));
            var hoje = calculadora.calcular(VERBA, List.of(), eventos, em(3));

            assertThat(hoje.eventosProximos()).hasSize(1);
            assertThat(hoje.mensagem()).contains("Viagem de sábado");
        }
    }

    @Nested
    @DisplayName("RN-19: a verba responde ao que já foi gasto")
    class VerbaQueResponde {

        @Test
        @DisplayName("segurou ontem, a verba de hoje cresce")
        void segurouOntem() {
            var hoje = calculadora.calcular(VERBA, List.of(gasto(1, "20")), List.of(), em(2));
            assertThat(hoje.verbaDeHoje())
                    .isGreaterThan(VERBA.verbaBase())
                    .isEqualTo(Dinheiro.de("94.14"));
        }

        @Test
        @DisplayName("gastou ontem, a verba de hoje encolhe")
        void gastouOntem() {
            var hoje = calculadora.calcular(VERBA, List.of(gasto(1, "300")), List.of(), em(2));
            assertThat(hoje.verbaDeHoje())
                    .isLessThan(VERBA.verbaBase())
                    .isEqualTo(Dinheiro.de("84.48"));
        }

        @Test
        @DisplayName("o gasto de hoje não encolhe a verba de hoje, mas já sai do restante")
        void gastoDeHojeNaoRealimenta() {
            var hoje = calculadora.calcular(VERBA, List.of(gasto(10, "500")), List.of(), em(10));

            assertThat(hoje.verbaDeHoje()).isEqualTo(Dinheiro.de("130.95"));
            assertThat(hoje.gastoAteHoje()).isEqualTo(Dinheiro.de(500));
            assertThat(hoje.restanteDoMes()).isEqualTo(Dinheiro.de(2250));
        }

        @Test
        @DisplayName("no último dia toda a verba restante está disponível")
        void ultimoDia() {
            var hoje = calculadora.calcular(VERBA, List.of(), List.of(), em(30));
            assertThat(hoje.diasRestantes()).isEqualTo(1);
            assertThat(hoje.verbaDeHoje()).isEqualTo(Dinheiro.de(2750));
        }
    }

    @Nested
    @DisplayName("ritmo e semáforo")
    class RitmoESemaforo {

        @Test
        @DisplayName("gastar rápido derruba a faixa antes de o mês acabar")
        void ritmoAcelerado() {
            List<GastoDoDia> gastos = new ArrayList<>();
            for (int dia = 1; dia <= 5; dia++) {
                gastos.add(gasto(dia, "200"));
            }
            var hoje = calculadora.calcular(VERBA, gastos, List.of(), em(5));

            assertThat(hoje.gastoAteHoje()).isEqualTo(Dinheiro.de(1000));
            assertThat(hoje.ritmo()).isEqualByComparingTo("2.18");
            assertThat(hoje.faixa()).isNotEqualTo(FaixaSaude.IDEAL);
            assertThat(hoje.baixaConfianca()).isFalse();
        }

        @Test
        @DisplayName("dias 1 e 2 calculam o ritmo mas não alertam")
        void inicioDoMes() {
            assertThat(calculadora.calcular(VERBA, List.of(), List.of(), em(1)).baixaConfianca()).isTrue();
            assertThat(calculadora.calcular(VERBA, List.of(), List.of(), em(2)).baixaConfianca()).isTrue();
            assertThat(calculadora.calcular(VERBA, List.of(), List.of(), em(3)).baixaConfianca()).isFalse();
        }

        @Test
        @DisplayName("as faixas seguem os limiares da RN-19")
        void faixas() {
            assertThat(FaixaSaude.de(new java.math.BigDecimal("1.00"))).isEqualTo(FaixaSaude.IDEAL);
            assertThat(FaixaSaude.de(new java.math.BigDecimal("0.85"))).isEqualTo(FaixaSaude.SEGUINDO_BEM);
            assertThat(FaixaSaude.de(new java.math.BigDecimal("0.70"))).isEqualTo(FaixaSaude.RUIM);
            assertThat(FaixaSaude.de(new java.math.BigDecimal("0.59"))).isEqualTo(FaixaSaude.PESSIMO);
        }
    }

    @Nested
    @DisplayName("tradução em ações")
    class Traducao {

        @Test
        @DisplayName("a verba vira coisas que se faz, com plural correto")
        void traduzEmAcoes() {
            var hoje = calculadora.calcular(VERBA, List.of(), List.of(), em(1));
            assertThat(hoje.podeFazer()).hasSize(3);
            assertThat(hoje.podeFazer().get(0).frase()).isEqualTo("2 refeições fora de R$ 38,16");
            assertThat(AcaoPossivel.de(TicketMedio.medidosEmAgosto2026().get(0), 1).frase())
                    .isEqualTo("1 refeição fora de R$ 38,16");
        }

        @Test
        @DisplayName("sem verba não há ação sugerida")
        void semVerba() {
            var hoje = calculadora.calcular(VERBA, List.of(gasto(1, "3000")), List.of(), em(15));
            assertThat(hoje.podeFazer()).isEmpty();
            assertThat(hoje.mensagem()).contains("só o essencial");
        }
    }

    @Nested
    @DisplayName("bordas")
    class Bordas {

        @Test
        @DisplayName("a verba nunca fica negativa")
        void nuncaNegativa() {
            var hoje = calculadora.calcular(VERBA, List.of(gasto(1, "5000")), List.of(), em(15));
            assertThat(hoje.verbaDeHoje()).isEqualTo(Dinheiro.ZERO);
            assertThat(hoje.restanteDoMes()).isEqualTo(Dinheiro.ZERO);
            assertThat(hoje.verbaAcabou()).isTrue();
            assertThat(hoje.faixa()).isEqualTo(FaixaSaude.PESSIMO);
        }

        @Test
        @DisplayName("gasto de outro mês é ignorado")
        void gastoDeOutroMes() {
            var deAgosto = new GastoDoDia(LocalDate.of(2026, 8, 20), Dinheiro.de(999));
            var hoje = calculadora.calcular(VERBA, List.of(deAgosto), List.of(), em(10));
            assertThat(hoje.gastoAteHoje()).isEqualTo(Dinheiro.ZERO);
        }

        @Test
        @DisplayName("calcular com o relógio fora da competência é erro de programação")
        void mesErrado() {
            var outubro = Clock.fixed(
                    LocalDate.of(2026, 10, 1).atStartOfDay(Competencia.FUSO).toInstant(), Competencia.FUSO);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> calculadora.calcular(VERBA, List.of(), List.of(), outubro));
        }

        @Test
        @DisplayName("fevereiro tem 28 dias e a verba diária muda")
        void fevereiro() {
            var fev = new VerbaMensal(Competencia.de(2026, 2), Dinheiro.de(3000), Dinheiro.de(250));
            assertThat(fev.verbaBase()).isEqualTo(Dinheiro.de("98.21"));
        }

        @Test
        @DisplayName("gasto e evento com valor negativo são erro de programação")
        void valoresNegativos() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new GastoDoDia(LocalDate.of(2026, 9, 1), Dinheiro.de(-1)));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Evento.previsto(LocalDate.of(2026, 9, 1), "x", Dinheiro.de(-1)));
        }
    }
}

package br.com.felipe.termometro.planoajuste.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MotorDoPlanoDeAjusteTest {

    private static final BigDecimal FATOR_35 = new BigDecimal("0.35");
    private static final Competencia SET_2026 = Competencia.de(2026, 9);

    @Nested
    @DisplayName("cenário Gherkin: vermelho não tem rampa")
    class VermelhoNaoTemRampa {

        @Test
        @DisplayName("gasto mediano de R$ 300 em transações VERMELHA vira alvo R$ 0 no mês 1")
        void vermelhoZeraNoMesUm() {
            GastoDaCategoria categoria = new GastoDaCategoria("RESTAURANTE", List.of(),
                    List.of(Dinheiro.de("280.00"), Dinheiro.de("300.00"), Dinheiro.de("320.00")), null);

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(categoria), FATOR_35, 3);

            assertThat(plano.itens()).hasSize(1);
            ItemDoPlano item = plano.itens().get(0);
            assertThat(item.tipo()).isEqualTo(ItemDoPlano.TipoDeItem.ZERAR_VERMELHO);
            assertThat(item.valorAtual().valor()).isEqualByComparingTo("300.00");
            assertThat(item.alvoFinal().valor()).isEqualByComparingTo("0.00");
            assertThat(item.dor()).isEqualTo(1);
            assertThat(item.alvosMensais()).hasSize(1);
            assertThat(item.alvosMensais().get(0).mes()).isEqualTo(1);
            assertThat(item.alvosMensais().get(0).alvo().valor()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("categoria variável (azul+amarelo) ramando até o piso")
    class RampaDaCategoriaVariavel {

        @Test
        @DisplayName("categoria acima do piso entra no plano com o item de rampa")
        void categoriaAcimaDoPisoEntraNoPlano() {
            GastoDaCategoria categoria = new GastoDaCategoria("RESTAURANTE",
                    List.of(Dinheiro.de("1200.00"), Dinheiro.de("1240.00"), Dinheiro.de("1300.00")),
                    List.of(), Dinheiro.de("320.00"));

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(categoria), FATOR_35, 3);

            assertThat(plano.itens()).hasSize(1);
            ItemDoPlano item = plano.itens().get(0);
            assertThat(item.tipo()).isEqualTo(ItemDoPlano.TipoDeItem.RAMPA_VARIAVEL);
            assertThat(item.valorAtual().valor()).isEqualByComparingTo("1240.00");
            assertThat(item.alvoFinal().valor()).isEqualByComparingTo("320.00");
            assertThat(item.dor()).isEqualTo(2);
            assertThat(item.rampaAlongada()).isTrue();
            assertThat(item.alvosMensais()).hasSize(4);
            assertThat(plano.avisos()).anyMatch(a -> a.contains("RESTAURANTE") && a.contains("alongada"));
        }

        @Test
        @DisplayName("categoria já dentro do piso (atual <= piso) não entra no plano")
        void categoriaDentroDoPisoNaoEntra() {
            GastoDaCategoria categoria = new GastoDaCategoria("TRANSPORTE",
                    List.of(Dinheiro.de("150.00"), Dinheiro.de("160.00"), Dinheiro.de("170.00")),
                    List.of(), Dinheiro.de("180.00"));

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(categoria), FATOR_35, 3);

            assertThat(plano.itens()).isEmpty();
            assertThat(plano.avisos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("edge cases de piso")
    class EdgeCasesDePiso {

        @Test
        @DisplayName("categoria sem piso humano (NAO_TRIADA) é pulada com aviso")
        void categoriaSemPisoEPulada() {
            GastoDaCategoria categoria = new GastoDaCategoria("HOBBY_NOVO",
                    List.of(Dinheiro.de("100.00"), Dinheiro.de("120.00"), Dinheiro.de("90.00")),
                    List.of(), null);

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(categoria), FATOR_35, 3);

            assertThat(plano.itens()).isEmpty();
            assertThat(plano.avisos()).anyMatch(a -> a.contains("HOBBY_NOVO") && a.contains("sem piso"));
        }

        @Test
        @DisplayName("categoria com piso zero é pulada com aviso, não quebra o cálculo")
        void categoriaComPisoZeroEPulada() {
            GastoDaCategoria categoria = new GastoDaCategoria("ASSINATURA_INUTIL",
                    List.of(Dinheiro.de("50.00"), Dinheiro.de("50.00"), Dinheiro.de("50.00")),
                    List.of(), Dinheiro.ZERO);

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(categoria), FATOR_35, 3);

            assertThat(plano.itens()).isEmpty();
            assertThat(plano.avisos())
                    .anyMatch(a -> a.contains("ASSINATURA_INUTIL") && a.contains("R$ 0,00"));
        }
    }

    @Nested
    @DisplayName("priorização por impacto = economiaMensal / dor")
    class Priorizacao {

        @Test
        @DisplayName("quatro itens gerados, top-3 por impacto em ordem decrescente")
        void topTresPorImpactoEmOrdemDecrescente() {
            // impactos: restaurante-rampa 920/2=460 · lazer-rampa 900/2=450 ·
            //           restaurante-vermelho 300/1=300 · transporte-rampa 130/2=65
            GastoDaCategoria restaurante = new GastoDaCategoria("RESTAURANTE",
                    List.of(Dinheiro.de("1200.00"), Dinheiro.de("1240.00"), Dinheiro.de("1300.00")),
                    List.of(Dinheiro.de("280.00"), Dinheiro.de("300.00"), Dinheiro.de("320.00")),
                    Dinheiro.de("320.00"));
            GastoDaCategoria transporte = new GastoDaCategoria("TRANSPORTE",
                    List.of(Dinheiro.de("300.00"), Dinheiro.de("310.00"), Dinheiro.de("320.00")),
                    List.of(), Dinheiro.de("180.00"));
            GastoDaCategoria lazer = new GastoDaCategoria("LAZER",
                    List.of(Dinheiro.de("980.00"), Dinheiro.de("1000.00"), Dinheiro.de("1020.00")),
                    List.of(), Dinheiro.de("100.00"));

            PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(
                    SET_2026, List.of(restaurante, transporte, lazer), FATOR_35, 3);

            // 4 itens no total (3 rampas variáveis + 1 vermelho), só as 3 de maior impacto aparecem
            assertThat(plano.itens()).hasSize(4);
            assertThat(plano.acoesPrioritarias()).hasSize(3);

            assertThat(plano.acoesPrioritarias().get(0).categoria()).isEqualTo("RESTAURANTE");
            assertThat(plano.acoesPrioritarias().get(0).dor()).isEqualTo(2);
            assertThat(plano.acoesPrioritarias().get(1).categoria()).isEqualTo("LAZER");
            assertThat(plano.acoesPrioritarias().get(2).categoria()).isEqualTo("RESTAURANTE");
            assertThat(plano.acoesPrioritarias().get(2).dor()).isEqualTo(1);
            // transporte (impacto 65) fica de fora do top-3
            assertThat(plano.acoesPrioritarias()).noneMatch(a -> a.categoria().equals("TRANSPORTE"));

            // impacto é decrescente
            for (int i = 1; i < plano.acoesPrioritarias().size(); i++) {
                assertThat(plano.acoesPrioritarias().get(i - 1).impacto())
                        .isGreaterThanOrEqualTo(plano.acoesPrioritarias().get(i).impacto());
            }
        }

        @Test
        @DisplayName("dor menor vence a igualdade de economia mensal")
        void dorMenorVenceAIgualdadeDeEconomia() {
            // vermelho: economia 200, dor 1, impacto 200
            GastoDaCategoria impulso = new GastoDaCategoria("LAZER",
                    List.of(), List.of(Dinheiro.de("190.00"), Dinheiro.de("200.00"), Dinheiro.de("210.00")),
                    null);
            // rampa: atual 400 -> piso 200, economia 200, dor 2, impacto 100
            GastoDaCategoria habito = new GastoDaCategoria("COMPRAS_ONLINE",
                    List.of(Dinheiro.de("380.00"), Dinheiro.de("400.00"), Dinheiro.de("420.00")),
                    List.of(), Dinheiro.de("200.00"));

            PlanoDeAjuste plano =
                    MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(impulso, habito), FATOR_35, 3);

            assertThat(plano.acoesPrioritarias()).hasSize(2);
            assertThat(plano.acoesPrioritarias().get(0).categoria()).isEqualTo("LAZER");
            assertThat(plano.acoesPrioritarias().get(0).dor()).isEqualTo(1);
            assertThat(plano.acoesPrioritarias().get(0).economiaMensal().valor())
                    .isEqualByComparingTo(plano.acoesPrioritarias().get(1).economiaMensal().valor());
            assertThat(plano.acoesPrioritarias().get(0).impacto())
                    .isGreaterThan(plano.acoesPrioritarias().get(1).impacto());
        }

        @Test
        @DisplayName("economiaMensalFinalTotal soma a economia de todos os itens, inclusive os fora do top-3")
        void economiaTotalSomaTodosOsItens() {
            GastoDaCategoria restaurante = new GastoDaCategoria("RESTAURANTE",
                    List.of(Dinheiro.de("1200.00"), Dinheiro.de("1240.00"), Dinheiro.de("1300.00")),
                    List.of(), Dinheiro.de("320.00"));
            GastoDaCategoria transporte = new GastoDaCategoria("TRANSPORTE",
                    List.of(Dinheiro.de("300.00"), Dinheiro.de("310.00"), Dinheiro.de("320.00")),
                    List.of(), Dinheiro.de("180.00"));

            PlanoDeAjuste plano =
                    MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(restaurante, transporte), FATOR_35, 3);

            Dinheiro esperado = Dinheiro.de("1240.00").subtrair(Dinheiro.de("320.00"))
                    .somar(Dinheiro.de("310.00").subtrair(Dinheiro.de("180.00")));
            assertThat(plano.economiaMensalFinalTotal().valor()).isEqualByComparingTo(esperado.valor());
        }
    }

    @Test
    @DisplayName("nenhuma categoria: plano vazio, sem avisos, economia zero")
    void nenhumaCategoriaDevolvePlanoVazio() {
        PlanoDeAjuste plano = MotorDoPlanoDeAjuste.gerar(SET_2026, List.of(), FATOR_35, 3);

        assertThat(plano.itens()).isEmpty();
        assertThat(plano.avisos()).isEmpty();
        assertThat(plano.acoesPrioritarias()).isEmpty();
        assertThat(plano.economiaMensalFinalTotal().valor()).isEqualByComparingTo("0.00");
    }
}

package br.com.felipe.termometro.triagem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MotorDeTriagem")
class MotorDeTriagemTest {

    @Nested
    @DisplayName("triar")
    class Triar {

        @Test
        @DisplayName("FIXO é sempre AZUL, mesmo sem piso definido")
        void fixoSempreAzul() {
            TransacaoClassificada aluguel = transacao("2026-09-05", "1500.00", "ALUGUEL", Natureza.FIXO, null);

            Map<UUID, Etiqueta> resultado = MotorDeTriagem.triar(List.of(aluguel), Map.of());

            assertThat(resultado.get(aluguel.id())).isEqualTo(Etiqueta.AZUL);
        }

        @Test
        @DisplayName("NAO_E_GASTO é sempre VERDE")
        void naoEGastoSempreVerde() {
            TransacaoClassificada amortizacao =
                    transacao("2026-09-05", "500.00", "EMPRESTIMO", Natureza.NAO_E_GASTO, null);

            Map<UUID, Etiqueta> resultado = MotorDeTriagem.triar(List.of(amortizacao), Map.of());

            assertThat(resultado.get(amortizacao.id())).isEqualTo(Etiqueta.VERDE);
        }

        @Test
        @DisplayName("VARIAVEL sem piso definido vira NAO_TRIADA")
        void variavelSemPisoViraNaoTriada() {
            TransacaoClassificada semPiso = transacao("2026-09-05", "45.00", "LAZER", Natureza.VARIAVEL, null);

            Map<UUID, Etiqueta> resultado = MotorDeTriagem.triar(List.of(semPiso), Map.of());

            assertThat(resultado.get(semPiso.id())).isEqualTo(Etiqueta.NAO_TRIADA);
        }

        @Test
        @DisplayName("VARIAVEL com piso roda o algoritmo do piso")
        void variavelComPisoRodaAlgoritmo() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t2 = transacao("2026-09-17", "80.00", "RESTAURANTE", Natureza.VARIAVEL, null);

            Map<UUID, Etiqueta> resultado =
                    MotorDeTriagem.triar(List.of(t1, t2), Map.of("RESTAURANTE", Dinheiro.de("160.00")));

            assertThat(resultado.get(t1.id())).isEqualTo(Etiqueta.AZUL);
            assertThat(resultado.get(t2.id())).isEqualTo(Etiqueta.AZUL);
        }

        @Test
        @DisplayName("promoção manual para VERMELHA nunca é sobrescrita pelo algoritmo automático")
        void vermelhaManualNuncaSobrescrita() {
            TransacaoClassificada promovida =
                    transacao("2026-09-03", "80.00", "RESTAURANTE", Natureza.VARIAVEL, Etiqueta.VERMELHA);

            Map<UUID, Etiqueta> resultado =
                    MotorDeTriagem.triar(List.of(promovida), Map.of("RESTAURANTE", Dinheiro.de("160.00")));

            assertThat(resultado.get(promovida.id())).isEqualTo(Etiqueta.VERMELHA);
        }

        @Test
        @DisplayName("categorias com naturezas divergentes são um erro de inconsistência")
        void naturezasDivergentesEhErro() {
            TransacaoClassificada a = transacao("2026-09-03", "80.00", "X", Natureza.VARIAVEL, null);
            TransacaoClassificada b = transacao("2026-09-04", "80.00", "X", Natureza.FIXO, null);

            assertThatThrownBy(() -> MotorDeTriagem.triar(List.of(a, b), Map.of()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("resumir")
    class Resumir {

        @Test
        @DisplayName("bate com o cenário Gherkin: split 160 azul / 80 amarelo")
        void resumoComSplitDaFronteira() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t2 = transacao("2026-09-10", "100.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t3 = transacao("2026-09-20", "60.00", "RESTAURANTE", Natureza.VARIAVEL, null);

            List<ResumoDeCategoria> resumo =
                    MotorDeTriagem.resumir(List.of(t1, t2, t3), Map.of("RESTAURANTE", Dinheiro.de("160.00")));

            ResumoDeCategoria restaurante = resumo.getFirst();
            assertThat(restaurante.totalAzul()).isEqualTo(Dinheiro.de("160.00"));
            assertThat(restaurante.totalAmarelo()).isEqualTo(Dinheiro.de("80.00"));
            assertThat(restaurante.totalVermelho()).isEqualTo(Dinheiro.ZERO);
        }

        @Test
        @DisplayName("promover uma transação totalmente amarela move o valor inteiro pro vermelho, sem tocar o azul")
        void promocaoDeTransacaoTotalmenteAmarela() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t2 = transacao("2026-09-10", "100.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t3 = transacao("2026-09-20", "60.00", "RESTAURANTE", Natureza.VARIAVEL,
                    Etiqueta.VERMELHA);

            List<ResumoDeCategoria> resumo =
                    MotorDeTriagem.resumir(List.of(t1, t2, t3), Map.of("RESTAURANTE", Dinheiro.de("160.00")));

            ResumoDeCategoria restaurante = resumo.getFirst();
            assertThat(restaurante.totalAzul()).isEqualTo(Dinheiro.de("160.00"));
            assertThat(restaurante.totalAmarelo()).isEqualTo(Dinheiro.de("20.00"));
            assertThat(restaurante.totalVermelho()).isEqualTo(Dinheiro.de("60.00"));
        }

        @Test
        @DisplayName("promover a própria transação-fronteira move o valor inteiro (azul + amarelo) pro vermelho")
        void promocaoDaTransacaoFronteira() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00", "RESTAURANTE", Natureza.VARIAVEL, null);
            TransacaoClassificada t2 = transacao("2026-09-10", "100.00", "RESTAURANTE", Natureza.VARIAVEL,
                    Etiqueta.VERMELHA);
            TransacaoClassificada t3 = transacao("2026-09-20", "60.00", "RESTAURANTE", Natureza.VARIAVEL, null);

            List<ResumoDeCategoria> resumo =
                    MotorDeTriagem.resumir(List.of(t1, t2, t3), Map.of("RESTAURANTE", Dinheiro.de("160.00")));

            ResumoDeCategoria restaurante = resumo.getFirst();
            assertThat(restaurante.totalAzul()).isEqualTo(Dinheiro.de("80.00"));
            assertThat(restaurante.totalAmarelo()).isEqualTo(Dinheiro.de("60.00"));
            assertThat(restaurante.totalVermelho()).isEqualTo(Dinheiro.de("100.00"));
        }

        @Test
        @DisplayName("FIXO soma inteiro no azul")
        void fixoSomaNoAzul() {
            TransacaoClassificada aluguel = transacao("2026-09-05", "1500.00", "ALUGUEL", Natureza.FIXO, null);

            List<ResumoDeCategoria> resumo = MotorDeTriagem.resumir(List.of(aluguel), Map.of());

            assertThat(resumo.getFirst().totalAzul()).isEqualTo(Dinheiro.de("1500.00"));
        }

        @Test
        @DisplayName("NAO_E_GASTO soma inteiro no verde")
        void naoEGastoSomaNoVerde() {
            TransacaoClassificada amortizacao =
                    transacao("2026-09-05", "500.00", "EMPRESTIMO", Natureza.NAO_E_GASTO, null);

            List<ResumoDeCategoria> resumo = MotorDeTriagem.resumir(List.of(amortizacao), Map.of());

            assertThat(resumo.getFirst().totalVerde()).isEqualTo(Dinheiro.de("500.00"));
        }

        @Test
        @DisplayName("VARIAVEL sem piso soma no não-triada")
        void variavelSemPisoSomaNoNaoTriada() {
            TransacaoClassificada semPiso = transacao("2026-09-05", "45.00", "LAZER", Natureza.VARIAVEL, null);

            List<ResumoDeCategoria> resumo = MotorDeTriagem.resumir(List.of(semPiso), Map.of());

            assertThat(resumo.getFirst().totalNaoTriada()).isEqualTo(Dinheiro.de("45.00"));
        }
    }

    private static TransacaoClassificada transacao(String data, String valor, String categoria,
                                                    Natureza natureza, Etiqueta etiquetaAtual) {
        return new TransacaoClassificada(UUID.randomUUID(), LocalDate.parse(data), Dinheiro.de(valor),
                categoria, natureza, etiquetaAtual);
    }
}

package br.com.felipe.termometro.classificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Categorizador")
class CategorizadorTest {

    private final Categorizador categorizador = Categorizador.padrao();

    private static TransacaoBruta gasto(String descricao, String valor) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 10), null, descricao, descricao,
                Dinheiro.de(valor), null, null, SecaoFatura.CARTAO, null, Origem.CSV, 0);
    }

    @ParameterizedTest(name = "{0} -> {1} (na verba: {2})")
    @DisplayName("classifica as transações reais das faturas de jun–ago/2026")
    @CsvSource({
            "'Pix no Crédito - Nu Pagamentos SA', ROLAGEM_DE_DIVIDA, false",
            "'Pagamento recebido',                PAGAMENTO_DE_FATURA, false",
            "'PIX Ministerio Da Fazen',           IMPOSTO_PJ,        false",
            "'Sitecnet*Tely',                     INTERNET,          false",
            "'ANTHROPIC* CLAUDE SUB',             ASSINATURA,        false",
            "'Smartblue Jp',                      LAVANDERIA,        true",
            "'IFD*RESTAURANTE MINERIM',           DELIVERY,          true",
            "'RESTAURANTE MINERIM',               RESTAURANTE,       true",
            "'Supermercado Arruda',               MERCADO,           true",
            "'Panificadora Sao Gonca',            PADARIA,           true",
            "'UBER * PENDING',                    TRANSPORTE_APP,    true",
            "'99*SAO PAULOBR',                    TRANSPORTE_APP,    true",
            "'Pague Menos',                       FARMACIA,          true",
            "'CINEPOLIS MANAIRA',                 LAZER,             true",
            "'NOHA SHOES - J',                    VESTUARIO,         true",
            "'Amazon BR IV - NuPay',              COMPRAS_ONLINE,    true",
            "'Loja Casa e Condominio',            NAO_IDENTIFICADA,  true",
    })
    void classificaTransacoesReais(String descricao, String categoria, boolean naVerba) {
        Classificacao resultado = categorizador.classificar(gasto(descricao, "-50.00"));
        assertThat(resultado.categoria().nome()).isEqualTo(categoria);
        assertThat(resultado.contaNoDiaADia()).isEqualTo(naVerba);
    }

    @Nested
    @DisplayName("a ordem das regras decide")
    class Ordem {

        @Test
        @DisplayName("delivery é avaliado antes de restaurante")
        void deliveryAntesDeRestaurante() {
            assertThat(categorizador.classificar(gasto("IFD*Poke Alimentos", "-41.89")).categoria())
                    .as("IFD*RESTAURANTE é delivery; se virar presencial, a RN-13 mede o padrão errado")
                    .isEqualTo(CatalogoDeRegrasPadrao.DELIVERY);
        }

        @Test
        @DisplayName("rolagem de dívida é avaliada antes de qualquer coisa com 'Pagamento'")
        void rolagemAntesDePagamento() {
            assertThat(categorizador.classificar(gasto("PIX Nu Pagamentos SA", "-2436.11")).categoria())
                    .isEqualTo(CatalogoDeRegrasPadrao.ROLAGEM_DE_DIVIDA);
        }
    }

    @Nested
    @DisplayName("RN-19: o que não entra na verba diária")
    class VerbaDiaria {

        @Test
        @DisplayName("parcela é decisão de um mês passado")
        void parcelaNaoEntra() {
            TransacaoBruta parcelada = new TransacaoBruta(LocalDate.of(2026, 9, 5), null,
                    "Orange Shopping", "Orange Shopping - Parcela 9/10", Dinheiro.de("-198.80"),
                    null, null, SecaoFatura.CARTAO, new Parcela(9, 10), Origem.CSV, 0);
            assertThat(categorizador.classificar(parcelada).contaNoDiaADia())
                    .as("contar a parcela hoje pune o usuário duas vezes pela mesma compra")
                    .isFalse();
        }

        @Test
        @DisplayName("fixo, pagamento e crédito ficam de fora; variável entra")
        void demaisCasos() {
            assertThat(categorizador.classificar(gasto("Sitecnet*Tely", "-129.90")).contaNoDiaADia()).isFalse();
            assertThat(categorizador.classificar(gasto("Estorno", "20.00")).contaNoDiaADia()).isFalse();
            assertThat(categorizador.classificar(gasto("Supermercado Arruda", "-64.25")).contaNoDiaADia()).isTrue();
        }

        @Test
        @DisplayName("parcela futura não compõe o mês")
        void parcelaFutura() {
            TransacaoBruta futura = new TransacaoBruta(LocalDate.of(2026, 9, 5), null, "Noha Shoes",
                    "Noha Shoes", Dinheiro.de("-314.95"), null, null, SecaoFatura.FUTURO, null,
                    Origem.PDF, 0);
            assertThat(categorizador.classificar(futura).contaNoDiaADia()).isFalse();
        }
    }

    @Nested
    @DisplayName("confiança e fila de revisão (RN-12)")
    class Confianca {

        @Test
        @DisplayName("regra do catálogo por descrição fica em 0,72 e decide sozinha")
        void regraDoCatalogo() {
            Classificacao mercado = categorizador.classificar(gasto("Supermercado Arruda", "-64.25"));
            assertThat(mercado.confianca()).isEqualByComparingTo("0.72");
            assertThat(mercado.confiavel()).isTrue();
            assertThat(mercado.precisaRevisao()).isFalse();
        }

        @Test
        @DisplayName("o que nenhuma regra pega vai para a fila, com o motivo")
        void naoIdentificada() {
            Classificacao resultado = categorizador.classificar(gasto("Loja Casa e Condominio", "-8.98"));
            assertThat(resultado.categoria()).isEqualTo(Categoria.NAO_IDENTIFICADA);
            assertThat(resultado.precisaRevisao()).isTrue();
            assertThat(resultado.motivo()).contains("nenhuma regra casou");
        }
    }

    @Nested
    @DisplayName("precedência: usuário > aprendizado > catálogo")
    class Precedencia {

        @Test
        @DisplayName("uma correção do usuário vira regra e resolve o grupo inteiro")
        void aprendizadoVenceOCatalogo() {
            Categorizador comAprendizado = categorizador.com(List.of(
                    RegraDeCategorizacao.aprendida("LOJA CASA E CONDOMINIO", CatalogoDeRegrasPadrao.MERCADO)));

            Classificacao resultado = comAprendizado.classificar(gasto("Loja Casa e Condominio", "-8.98"));

            assertThat(resultado.categoria()).isEqualTo(CatalogoDeRegrasPadrao.MERCADO);
            assertThat(resultado.origem()).isEqualTo(OrigemDaRegra.APRENDIZADO);
            assertThat(resultado.precisaRevisao()).isFalse();
        }

        @Test
        @DisplayName("a decisão explícita do usuário nunca é desfeita pelo aprendizado")
        void usuarioVenceAprendizado() {
            Categorizador completo = categorizador
                    .com(List.of(RegraDeCategorizacao.aprendida("LOJA CASA E CONDOMINIO",
                            CatalogoDeRegrasPadrao.MERCADO)))
                    .com(List.of(RegraDeCategorizacao.doUsuario(TipoDeRegra.ESTABELECIMENTO,
                            "LOJA CASA E CONDOMINIO", CatalogoDeRegrasPadrao.CONTAS_DE_CASA)));

            Classificacao resultado = completo.classificar(gasto("Loja Casa e Condominio", "-8.98"));

            assertThat(resultado.categoria()).isEqualTo(CatalogoDeRegrasPadrao.CONTAS_DE_CASA);
            assertThat(resultado.confianca()).isEqualByComparingTo("0.95");
            assertThat(resultado.contaNoDiaADia()).isFalse();
        }
    }

    @Test
    @DisplayName("a cidade colada pelo Itaú não atrapalha a classificação")
    void desgludeCidade() {
        TransacaoBruta doItau = new TransacaoBruta(LocalDate.of(2026, 9, 10), null,
                "SUPERMERCADO ARRUDAJOAO", "SUPERMERCADO ARRUDAJOAO", Dinheiro.de("-64.25"),
                "JOAO PESSOA", null, SecaoFatura.CARTAO, null, Origem.PDF, 0);
        assertThat(categorizador.classificar(doItau).categoria()).isEqualTo(CatalogoDeRegrasPadrao.MERCADO);
    }

    @Test
    @DisplayName("regex inválida em regra falha na construção, não em produção")
    void regexInvalida() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                RegraDeCategorizacao.doSistema(1, TipoDeRegra.DESCRICAO, "[", CatalogoDeRegrasPadrao.MERCADO));
    }
}

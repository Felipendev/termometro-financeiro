package br.com.felipe.termometro.diagnostico.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TesteDeViabilidade (RN-16)")
class TesteDeViabilidadeTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final Percentual META_25 = Percentual.dePontos("25");

    @Nested
    @DisplayName("veredito")
    class Veredito_ {

        @Test
        @DisplayName("VIÁVEL: números reais de Felipe (Premissas da planilha) batem exatamente")
        void viavelComOsNumerosReais() {
            // renda 10.000 · custo fixo 4.264,05 · piso variável 1.310,00 · meta 25%
            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(10000),
                    Dinheiro.de("4264.05"), Dinheiro.de("1310.00"), META_25, List.of());

            assertThat(viabilidade.custoMinimoVida()).isEqualTo(Dinheiro.de("5574.05"));
            assertThat(viabilidade.economiaMaxima()).isEqualTo(Dinheiro.de("4425.95"));
            assertThat(viabilidade.taxaMaxima()).isEqualTo(Percentual.deFracao("0.442595"));
            assertThat(viabilidade.veredito()).isEqualTo(Veredito.VIAVEL);
            assertThat(viabilidade.quedaDeRenda()).isNull();
            assertThat(viabilidade.leitura()).contains("Dá para bater a meta");
        }

        @Test
        @DisplayName("VIÁVEL_PARCIALMENTE: sobra menos que a meta, mas ainda sobra")
        void viavelParcialmente() {
            // exemplo do Gherkin da spec: renda 8.200 · fixo 5.400 · piso 1.900 · meta 25%
            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(8200),
                    Dinheiro.de(5400), Dinheiro.de(1900), META_25, List.of());

            assertThat(viabilidade.economiaMaxima()).isEqualTo(Dinheiro.de(900));
            assertThat(viabilidade.taxaMaxima().emPontos()).isEqualByComparingTo("10.9756");
            assertThat(viabilidade.veredito())
                    .isEqualTo(Veredito.VIAVEL_PARCIALMENTE);
            assertThat(viabilidade.alvoReducaoFixo()).isEqualTo(Dinheiro.de("1150.00"));
        }

        @Test
        @DisplayName("INVIÁVEL: custo mínimo de vida excede a renda — estrutural, não disciplina")
        void inviavel() {
            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(10000),
                    Dinheiro.de(6000), Dinheiro.de(4800), META_25, List.of());

            assertThat(viabilidade.economiaMaxima()).isEqualTo(Dinheiro.de(-800));
            assertThat(viabilidade.veredito()).isEqualTo(Veredito.INVIAVEL);
            assertThat(viabilidade.leitura()).contains("Nenhuma disciplina de gasto resolve");
        }

        @Test
        @DisplayName("economia máxima exatamente zero é INVIÁVEL, não parcial")
        void economiaZeroEhInviavel() {
            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(10000),
                    Dinheiro.de(6000), Dinheiro.de(4000), META_25, List.of());

            assertThat(viabilidade.economiaMaxima()).isEqualTo(Dinheiro.ZERO);
            assertThat(viabilidade.veredito()).isEqualTo(Veredito.INVIAVEL);
        }
    }

    @Nested
    @DisplayName("RN-16.1 — queda estrutural de renda")
    class QuedaDeRenda_ {

        @Test
        @DisplayName("menos de 6 meses de histórico: não dispara (edge case 30)")
        void naoDisparaComMenosDeSeisMeses() {
            List<Renda> tresmeses = List.of(
                    renda(Competencia.de(2026, 9), 10000),
                    renda(Competencia.de(2026, 8), 10000),
                    renda(Competencia.de(2026, 7), 10000));

            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(10000),
                    Dinheiro.de("4264.05"), Dinheiro.de("1310.00"), META_25, tresmeses);

            assertThat(viabilidade.quedaDeRenda()).isNull();
        }

        @Test
        @DisplayName("renda constante (o caso real de Felipe): 6 meses iguais não disparam")
        void naoDisparaComRendaConstante() {
            List<Renda> seisMesesIguais = List.of(
                    renda(Competencia.de(2026, 9), 10000), renda(Competencia.de(2026, 8), 10000),
                    renda(Competencia.de(2026, 7), 10000), renda(Competencia.de(2026, 6), 10000),
                    renda(Competencia.de(2026, 5), 10000), renda(Competencia.de(2026, 4), 10000));

            Viabilidade viabilidade = TesteDeViabilidade.calcular(SETEMBRO, Dinheiro.de(10000),
                    Dinheiro.de("4264.05"), Dinheiro.de("1310.00"), META_25, seisMesesIguais);

            assertThat(viabilidade.quedaDeRenda()).isNull();
        }

        @Test
        @DisplayName("queda de 28,6% (14k -> 10k) com 6 meses de histórico dispara e explica o peso do fixo")
        void disparaNaQuedaDeCatorzeParaDez() {
            List<Renda> historico = List.of(
                    renda(Competencia.de(2026, 8), 10000), renda(Competencia.de(2026, 7), 10000),
                    renda(Competencia.de(2026, 6), 10000),
                    renda(Competencia.de(2026, 5), 14000), renda(Competencia.de(2026, 4), 14000),
                    renda(Competencia.de(2026, 3), 14000));

            Viabilidade viabilidade = TesteDeViabilidade.calcular(Competencia.de(2026, 8),
                    Dinheiro.de(10000), Dinheiro.de(6300), Dinheiro.de(1310), META_25, historico);

            assertThat(viabilidade.quedaDeRenda()).isNotNull();
            QuedaDeRenda queda = viabilidade.quedaDeRenda();
            assertThat(queda.rendaAnterior()).isEqualTo(Dinheiro.de(14000));
            assertThat(queda.rendaAtual()).isEqualTo(Dinheiro.de(10000));
            assertThat(queda.quedaPct().emPontos()).isEqualByComparingTo("28.5714");
            assertThat(queda.pesoFixoAntes().emPontos()).isEqualByComparingTo("45.0000");
            assertThat(queda.pesoFixoAgora().emPontos()).isEqualByComparingTo("63.0000");
            assertThat(queda.mensagem()).contains("aritmética, não falta de disciplina");
        }

        @Test
        @DisplayName("queda abaixo do limiar de 15% não dispara")
        void quedaPequenaNaoDispara() {
            List<Renda> historico = List.of(
                    renda(Competencia.de(2026, 8), 9500), renda(Competencia.de(2026, 7), 9500),
                    renda(Competencia.de(2026, 6), 9500),
                    renda(Competencia.de(2026, 5), 10000), renda(Competencia.de(2026, 4), 10000),
                    renda(Competencia.de(2026, 3), 10000));

            Viabilidade viabilidade = TesteDeViabilidade.calcular(Competencia.de(2026, 8),
                    Dinheiro.de(9500), Dinheiro.de(4264), Dinheiro.de(1310), META_25, historico);

            assertThat(viabilidade.quedaDeRenda()).isNull();
        }

        private Renda renda(Competencia competencia, long valor) {
            return new Renda(competencia, Dinheiro.de(valor), null);
        }
    }

    @Test
    @DisplayName("renda líquida zero ou negativa é erro de programação, não de negócio")
    void rendaInvalida() {
        assertThatIllegalArgumentException().isThrownBy(() -> TesteDeViabilidade.calcular(SETEMBRO,
                Dinheiro.ZERO, Dinheiro.de(1000), Dinheiro.de(500), META_25, List.of()));
    }
}

package br.com.felipe.termometro.triagem.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AlgoritmoDoPiso")
class AlgoritmoDoPisoTest {

    private static final Dinheiro PISO_RESTAURANTE = Dinheiro.de("160.00");

    @Nested
    @DisplayName("dentro do piso")
    class DentroDoPiso {

        @Test
        @DisplayName("duas transações que somadas não passam do piso ficam ambas AZUL (cenário Gherkin 1)")
        void ambasDentroDoPiso() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00");
            TransacaoClassificada t2 = transacao("2026-09-17", "80.00");

            List<ResultadoDoPiso> resultado = AlgoritmoDoPiso.aplicar(List.of(t1, t2), PISO_RESTAURANTE);

            assertThat(resultado).hasSize(2);
            assertThat(resultado).allMatch(r -> r.etiqueta() == Etiqueta.AZUL);
            assertThat(somaPorParte(resultado, ResultadoDoPiso::parteAmarela)).isEqualTo(Dinheiro.ZERO);
        }
    }

    @Nested
    @DisplayName("cruza o piso")
    class CruzaOPiso {

        @Test
        @DisplayName("terceira transação estoura o piso — split lógico 160/80 (cenário Gherkin 2)")
        void splitNaFronteira() {
            TransacaoClassificada t1 = transacao("2026-09-03", "80.00");
            TransacaoClassificada t2 = transacao("2026-09-10", "100.00");
            TransacaoClassificada t3 = transacao("2026-09-20", "60.00");

            List<ResultadoDoPiso> resultado = AlgoritmoDoPiso.aplicar(List.of(t1, t2, t3), PISO_RESTAURANTE);

            assertThat(somaPorParte(resultado, ResultadoDoPiso::parteAzul)).isEqualTo(Dinheiro.de("160.00"));
            assertThat(somaPorParte(resultado, ResultadoDoPiso::parteAmarela)).isEqualTo(Dinheiro.de("80.00"));

            // a transação que cruza (t2) é etiquetada inteira como AMARELA — a divisão é só do agregado
            ResultadoDoPiso resultadoT2 = paraId(resultado, t2.id());
            assertThat(resultadoT2.etiqueta()).isEqualTo(Etiqueta.AMARELA);
            assertThat(resultadoT2.parteAzul()).isEqualTo(Dinheiro.de("80.00"));
            assertThat(resultadoT2.parteAmarela()).isEqualTo(Dinheiro.de("20.00"));

            assertThat(paraId(resultado, t1.id()).etiqueta()).isEqualTo(Etiqueta.AZUL);
            assertThat(paraId(resultado, t3.id()).etiqueta()).isEqualTo(Etiqueta.AMARELA);
        }

        @Test
        @DisplayName("acumulado que fecha exatamente no piso não cruza — a próxima é amarela inteira")
        void acumuladoExatoNoPisoNaoCruza() {
            TransacaoClassificada t1 = transacao("2026-09-03", "160.00");
            TransacaoClassificada t2 = transacao("2026-09-10", "50.00");

            List<ResultadoDoPiso> resultado = AlgoritmoDoPiso.aplicar(List.of(t1, t2), PISO_RESTAURANTE);

            assertThat(paraId(resultado, t1.id()).etiqueta()).isEqualTo(Etiqueta.AZUL);
            ResultadoDoPiso r2 = paraId(resultado, t2.id());
            assertThat(r2.etiqueta()).isEqualTo(Etiqueta.AMARELA);
            assertThat(r2.parteAzul()).isEqualTo(Dinheiro.ZERO);
            assertThat(r2.parteAmarela()).isEqualTo(Dinheiro.de("50.00"));
        }
    }

    @Nested
    @DisplayName("ordem")
    class Ordem {

        @Test
        @DisplayName("ordena por data, não pela ordem da lista de entrada")
        void ordenaPorData() {
            TransacaoClassificada maisRecente = transacao("2026-09-20", "60.00");
            TransacaoClassificada maisAntiga = transacao("2026-09-03", "80.00");

            List<ResultadoDoPiso> resultado =
                    AlgoritmoDoPiso.aplicar(List.of(maisRecente, maisAntiga), PISO_RESTAURANTE);

            assertThat(paraId(resultado, maisAntiga.id()).etiqueta()).isEqualTo(Etiqueta.AZUL);
            assertThat(paraId(resultado, maisRecente.id()).etiqueta()).isEqualTo(Etiqueta.AZUL);
        }
    }

    private static Dinheiro somaPorParte(List<ResultadoDoPiso> resultado, Function<ResultadoDoPiso, Dinheiro> parte) {
        return resultado.stream().map(parte).reduce(Dinheiro.ZERO, Dinheiro::somar);
    }

    private static ResultadoDoPiso paraId(List<ResultadoDoPiso> resultado, UUID id) {
        return resultado.stream().filter(r -> r.transacaoId().equals(id)).findFirst().orElseThrow();
    }

    private static TransacaoClassificada transacao(String data, String valor) {
        return new TransacaoClassificada(UUID.randomUUID(), LocalDate.parse(data), Dinheiro.de(valor),
                "RESTAURANTE", Natureza.VARIAVEL, null);
    }
}

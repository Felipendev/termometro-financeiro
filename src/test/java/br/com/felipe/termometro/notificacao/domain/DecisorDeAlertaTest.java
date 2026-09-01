package br.com.felipe.termometro.notificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DecisorDeAlerta — RN-22")
class DecisorDeAlertaTest {

    @Nested
    @DisplayName("verba baixa: só de novo se piorou")
    class VerbaBaixa {

        @Test
        @DisplayName("faixa boa nunca dispara, com ou sem aviso anterior")
        void faixaBoaNuncaDispara() {
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.IDEAL, null)).isFalse();
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.SEGUINDO_BEM, FaixaSaude.RUIM)).isFalse();
        }

        @Test
        @DisplayName("primeira vez em RUIM ou PÉSSIMO no dia dispara")
        void primeiraVezDispara() {
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.RUIM, null)).isTrue();
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.PESSIMO, null)).isTrue();
        }

        @Test
        @DisplayName("repetir a mesma faixa já avisada não dispara de novo")
        void mesmaFaixaNaoRepete() {
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.RUIM, FaixaSaude.RUIM)).isFalse();
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.PESSIMO, FaixaSaude.PESSIMO)).isFalse();
        }

        @Test
        @DisplayName("piorar de RUIM para PÉSSIMO no mesmo dia dispara de novo")
        void pioraDispara() {
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.PESSIMO, FaixaSaude.RUIM)).isTrue();
        }

        @Test
        @DisplayName("melhorar de PÉSSIMO para RUIM não dispara")
        void melhoraNaoDispara() {
            assertThat(DecisorDeAlerta.verbaPiorou(FaixaSaude.RUIM, FaixaSaude.PESSIMO)).isFalse();
        }
    }

    @Nested
    @DisplayName("transação alta")
    class TransacaoAlta {

        @Test
        @DisplayName("só despesa acima do limite entra — entrada/estorno e valor no limite ficam de fora")
        void filtraCorretamente() {
            List<TransacaoBruta> transacoes = List.of(
                    despesa("-150.00"),
                    despesa("-100.00"),
                    despesa("-99.99"),
                    entrada("150.00"));

            List<TransacaoBruta> altas =
                    DecisorDeAlerta.transacoesAcimaDoLimite(transacoes, Dinheiro.de("100"));

            assertThat(altas).extracting(t -> t.valor().paraJson()).containsExactly("-150.00");
        }
    }

    @Nested
    @DisplayName("marco atingido")
    class MarcoAtingido {

        private static final Competencia SETEMBRO = Competencia.de(2026, 9);

        @Test
        @DisplayName("marco nulo (não atingido dentro do horizonte) nunca dispara")
        void marcoNuloNaoDispara() {
            assertThat(DecisorDeAlerta.marcoAtingidoAgora(null, SETEMBRO)).isFalse();
        }

        @Test
        @DisplayName("marco igual à competência de hoje dispara")
        void marcoIgualAHojeDispara() {
            assertThat(DecisorDeAlerta.marcoAtingidoAgora(SETEMBRO, SETEMBRO)).isTrue();
        }

        @Test
        @DisplayName("marco em outra competência não dispara")
        void marcoEmOutraCompetenciaNaoDispara() {
            assertThat(DecisorDeAlerta.marcoAtingidoAgora(SETEMBRO.mais(1), SETEMBRO)).isFalse();
        }
    }

    private static TransacaoBruta despesa(String valor) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 10), null, "Compra", "Compra",
                Dinheiro.de(valor), null, null, SecaoFatura.CARTAO, null, Origem.CSV, 0);
    }

    private static TransacaoBruta entrada(String valor) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 10), null, "Pagamento recebido",
                "Pagamento recebido", Dinheiro.de(valor), null, null, SecaoFatura.MOVIMENTO_CONTA, null,
                Origem.CSV, 0);
    }

    @Test
    @DisplayName("mensagem de evento traz descrição, data e valor reservado")
    void mensagemDeEvento() {
        Evento evento = Evento.previsto(LocalDate.of(2026, 9, 25), "Aniversário", Dinheiro.de("170"));

        String mensagem = DecisorDeAlerta.mensagemEvento(evento);

        assertThat(mensagem).contains("Aniversário").contains("2026-09-25").contains("170,00");
    }
}

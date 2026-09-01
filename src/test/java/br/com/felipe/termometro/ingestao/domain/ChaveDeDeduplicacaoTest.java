package br.com.felipe.termometro.ingestao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChaveDeDeduplicacao")
class ChaveDeDeduplicacaoTest {

    private static final String CONTA = "itau-5000";
    private static final LocalDate DIA = LocalDate.of(2026, 7, 28);

    private static String chave(String descricao, String valor, int ordinal) {
        return ChaveDeDeduplicacao.calcular(CONTA, DIA, Dinheiro.de(valor), descricao, ordinal);
    }

    @Test
    @DisplayName("é determinística")
    void determinística() {
        assertThat(chave("Smartblue Jp", "-14.98", 0)).isEqualTo(chave("Smartblue Jp", "-14.98", 0));
    }

    @Test
    @DisplayName("o ordinal separa cobranças idênticas")
    void ordinalSepara() {
        assertThat(chave("Smartblue Jp", "-14.98", 0))
                .isNotEqualTo(chave("Smartblue Jp", "-14.98", 1));
    }

    @Test
    @DisplayName("a mesma compra descrita com e sem parcela colide de propósito")
    void normalizacaoUneVariantes() {
        assertThat(chave("Amazon - Parcela 9/12", "-82.75", 0))
                .as("a parcela muda todo mês; ela não pode fazer parte da identidade da transação")
                .isEqualTo(chave("Amazon", "-82.75", 0));
    }

    @Test
    @DisplayName("conta, data e valor entram na identidade")
    void componentesDaChave() {
        String base = chave("Supermercado Arruda", "-64.25", 0);
        assertThat(base).isNotEqualTo(chave("Supermercado Arruda", "-64.26", 0));
        assertThat(base).isNotEqualTo(ChaveDeDeduplicacao.calcular(
                "nubank-1", DIA, Dinheiro.de("-64.25"), "Supermercado Arruda", 0));
        assertThat(base).isNotEqualTo(ChaveDeDeduplicacao.calcular(
                CONTA, DIA.plusDays(1), Dinheiro.de("-64.25"), "Supermercado Arruda", 0));
    }

    @Test
    @DisplayName("ordinal negativo é erro de programação")
    void ordinalNegativo() {
        assertThatIllegalArgumentException().isThrownBy(() -> chave("X", "-1.00", -1));
    }
}

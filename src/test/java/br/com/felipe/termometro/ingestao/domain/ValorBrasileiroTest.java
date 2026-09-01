package br.com.felipe.termometro.ingestao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.shared.Dinheiro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ValorBrasileiro")
class ValorBrasileiroTest {

    @ParameterizedTest(name = "'{0}' -> {1}")
    @DisplayName("converte os formatos que os três bancos emitem")
    @CsvSource({
            "'14,96', 14.96",
            "'1.234,56', 1234.56",
            "'-3.212,29', -3212.29",
            "'- 2.625,03', -2625.03",
            "'R$ 5.627,73', 5627.73",
            "'0,00', 0.00",
    })
    void converte(String texto, String esperado) {
        assertThat(ValorBrasileiro.converter(texto)).isEqualTo(Dinheiro.de(esperado));
    }

    @Test
    @DisplayName("mil duzentos e trinta e quatro não vira um e vinte e três")
    void naoConfundeSeparadorDeMilhar() {
        assertThat(ValorBrasileiro.converter("1.234,56").centavos())
                .as("um replace(\",\", \".\") ingênuo daria 1.23")
                .isEqualTo(123_456L);
    }

    @ParameterizedTest
    @DisplayName("recusa o que não é valor brasileiro")
    @ValueSource(strings = {"1234.56", "abc", "12,345", "1,2", "", "05/06", "15,60%"})
    void recusa(String texto) {
        assertThat(ValorBrasileiro.pareceValor(texto)).isFalse();
        assertThatIllegalArgumentException().isThrownBy(() -> ValorBrasileiro.converter(texto));
    }
}

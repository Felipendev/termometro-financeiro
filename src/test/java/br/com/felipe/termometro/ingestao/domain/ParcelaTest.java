package br.com.felipe.termometro.ingestao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Parcela")
class ParcelaTest {

    @ParameterizedTest(name = "{0} -> {1}/{2}")
    @DisplayName("reconhece os três dialetos de parcelamento")
    @CsvSource({
            "'Amazon - Parcela 9/12', 9, 12",
            "'Pichau Informatica - NuPay - Parcela 10/12', 10, 12",
            "'AMAZON BR PARC10/10', 10, 10",
            "'MERCADOLIVRE*MPARC08/08', 8, 8",
            "'NOHA SHOES - J 01/04', 1, 4",
            "'JIM.COM 551015 03/04', 3, 4",
            "'AIRBNB * HM8MD 06/06', 6, 6",
            "'7ME IGREJA *IPARC02/03', 2, 3",
    })
    void reconhece(String descricao, int numero, int total) {
        assertThat(Parcela.extrairDe(descricao)).hasValue(new Parcela(numero, total));
    }

    @ParameterizedTest
    @DisplayName("não inventa parcela onde não há")
    @ValueSource(strings = {
            "SUPERMERCADO ARRUDA",
            "UBER * PENDING",
            "Pix no Crédito - RECEITA FEDERAL",
            "SMARTBLUE JP",
    })
    void naoInventa(String descricao) {
        assertThat(Parcela.extrairDe(descricao))
                .as("descrição sem parcelamento: %s", descricao)
                .isEmpty();
    }

    @Test
    @DisplayName("o sufixo NN/NN do Itaú é ambíguo com data, e a leitura correta é parcela")
    void sufixoNumericoDoItau() {
        // "AIRBNB * HM8MD 06/06" parece uma data, mas nas faturas reais é a 6ª de 6 parcelas:
        // o Itaú só repete lançamentos de meses anteriores quando eles são parcelados, e
        // "JIM.COM 551015" aparece como 03/04 em julho e 04/04 em agosto — sequência, não data.
        assertThat(Parcela.extrairDe("AIRBNB * HM8MD 06/06")).hasValue(new Parcela(6, 6));
        assertThat(Parcela.extrairDe("JIM.COM 551015 03/04")).hasValue(new Parcela(3, 4));
        assertThat(Parcela.extrairDe("JIM.COM 551015 04/04")).hasValue(new Parcela(4, 4));
    }

    @Test
    @DisplayName("calcula o que ainda vai vencer")
    void restantes() {
        assertThat(new Parcela(9, 12).restantes()).isEqualTo(3);
        assertThat(new Parcela(10, 10).restantes()).isZero();
        assertThat(new Parcela(10, 10).ehUltima()).isTrue();
    }

    @Test
    @DisplayName("rejeita parcela impossível")
    void rejeitaInvalida() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Parcela(13, 12));
        assertThatIllegalArgumentException().isThrownBy(() -> new Parcela(0, 12));
        assertThatIllegalArgumentException().isThrownBy(() -> new Parcela(1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new Parcela(1, 60));
    }
}

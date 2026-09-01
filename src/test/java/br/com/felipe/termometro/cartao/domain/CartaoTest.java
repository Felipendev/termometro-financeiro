package br.com.felipe.termometro.cartao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cartao")
class CartaoTest {

    private static Cartao construir(String nome, Dinheiro limite, Dinheiro valorFatura) {
        return new Cartao(UUID.randomUUID(), nome, limite, valorFatura, null, true);
    }

    @Test
    @DisplayName("nome em branco é rejeitado")
    void nomeEmBrancoEhRejeitado() {
        assertThatThrownBy(() -> construir("  ", null, Dinheiro.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");
    }

    @Test
    @DisplayName("limite negativo é rejeitado, mas limite nulo é aceito (cartão sem limite declarado)")
    void limiteNegativoEhRejeitado() {
        assertThatThrownBy(() -> construir("Nubank", Dinheiro.de("-1.00"), Dinheiro.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limite");

        assertThat(construir("Nubank", null, Dinheiro.ZERO).limiteOpcional()).isEmpty();
    }

    @Test
    @DisplayName("valor da fatura negativo é rejeitado")
    void valorDaFaturaNegativoEhRejeitado() {
        assertThatThrownBy(() -> construir("Nubank", null, Dinheiro.de("-50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fatura");
    }

    @Test
    @DisplayName("fatura zerada é aceita — cartão cadastrado sem gasto no mês")
    void faturaZeradaEhAceita() {
        assertThat(construir("Nubank", null, Dinheiro.ZERO).valorFatura()).isEqualTo(Dinheiro.ZERO);
    }
}

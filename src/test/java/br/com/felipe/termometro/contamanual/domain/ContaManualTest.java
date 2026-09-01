package br.com.felipe.termometro.contamanual.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContaManualTest {
    @Test
    void rejeitaIdentificadorVazioESaldoNulo() {
        assertThatThrownBy(() -> new ContaManual(UUID.randomUUID(), " ", "Conta", TipoContaManual.CORRENTE,
                Dinheiro.ZERO, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContaManual(UUID.randomUUID(), "principal", "Conta", TipoContaManual.CORRENTE,
                null, true)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void creditaEDebitaSemAlterarOsDadosDeCadastro() {
        UUID id = UUID.randomUUID();
        ContaManual conta = new ContaManual(id, "principal", "Conta", TipoContaManual.CORRENTE,
                Dinheiro.de("100"), true);

        ContaManual saldoFinal = conta.debitar(Dinheiro.de("32.40")).creditar(Dinheiro.de("10"));

        assertThat(saldoFinal.saldo()).isEqualTo(Dinheiro.de("77.60"));
        assertThat(saldoFinal.id()).isEqualTo(id);
        assertThat(saldoFinal.nome()).isEqualTo("Conta");
    }
}

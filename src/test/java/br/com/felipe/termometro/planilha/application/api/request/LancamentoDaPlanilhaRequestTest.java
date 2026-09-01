package br.com.felipe.termometro.planilha.application.api.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.lancamentoplanejado.domain.OrigemReceita;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LancamentoDaPlanilhaRequestTest {

    @Test
    void entradaUsaOrigemDeReceitaENaoCategoria() {
        var request = new LancamentoDaPlanilhaRequest(
                "Salário", "ENTRADA", new BigDecimal("5000"),
                null, null, null, "SALARIO");

        var item = request.paraDominio(UUID.randomUUID(), LocalDate.of(2026, 9, 5));

        assertThat(item.tipo()).isEqualTo(TipoLancamentoPlanejado.RECEITA);
        assertThat(item.origemReceita()).isEqualTo(OrigemReceita.SALARIO);
        assertThat(item.categoria()).isNull();
    }

    @Test
    void entradaComCategoriaEhRecusadaAntesDePersistir() {
        var request = new LancamentoDaPlanilhaRequest(
                "Salário", "ENTRADA", new BigDecimal("5000"),
                "Casa", "MORADIA", "FIXO", "SALARIO");

        assertThatThrownBy(() -> request.paraDominio(UUID.randomUUID(), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não possui categoria");
    }
}

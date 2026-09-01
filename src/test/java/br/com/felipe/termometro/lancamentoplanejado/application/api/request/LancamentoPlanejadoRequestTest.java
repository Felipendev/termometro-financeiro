package br.com.felipe.termometro.lancamentoplanejado.application.api.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LancamentoPlanejadoRequestTest {

    @Test
    void paraDominioMantemCategoriaECartaoEscolhidosNoFormulario() {
        UUID cartaoId = UUID.randomUUID();
        LancamentoPlanejadoRequest request = new LancamentoPlanejadoRequest(
                "Feira do bairro", "DESPESA", new BigDecimal("125.50"), LocalDate.of(2026, 8, 28),
                null, null, "Mercado", "ALIMENTACAO", "VARIAVEL", cartaoId);

        var resultado = request.paraDominio(UUID.randomUUID());

        assertThat(resultado.tipo()).isEqualTo(TipoLancamentoPlanejado.DESPESA);
        assertThat(resultado.categoria().nome()).isEqualTo("Mercado");
        assertThat(resultado.categoria().grupo()).isEqualTo("ALIMENTACAO");
        assertThat(resultado.categoria().natureza()).isEqualTo("VARIAVEL");
        assertThat(resultado.cartaoManualId()).isEqualTo(cartaoId);
    }

    @Test
    void converteMarcacaoDeCustoFixoEnviadaPeloModal() {
        LancamentoPlanejadoRequest request = new LancamentoPlanejadoRequest(
                "Aluguel", "DESPESA", new BigDecimal("2200.00"), LocalDate.of(2026, 8, 25),
                null, null, "Casa", "MORADIA", "FIXO", null, "CUSTO_FIXO");

        assertThat(request.paraDominio(UUID.randomUUID()).marcacaoPlanejamento().name())
                .isEqualTo("CUSTO_FIXO");
    }

    @Test
    void receitaUsaOrigemPropriaSemCategoriaDeDespesa() {
        LancamentoPlanejadoRequest request = new LancamentoPlanejadoRequest(
                "Salário", "RECEITA", new BigDecimal("5000.00"), LocalDate.of(2026, 9, 1),
                null, UUID.randomUUID(), null, null, null, null, null, "SALARIO");

        var resultado = request.paraDominio(UUID.randomUUID());

        assertThat(resultado.categoria()).isNull();
        assertThat(resultado.origemReceita().name()).isEqualTo("SALARIO");
    }

    @Test
    void receitaRejeitaCategoriaDeDespesa() {
        LancamentoPlanejadoRequest request = new LancamentoPlanejadoRequest(
                "Rendimento", "RECEITA", new BigDecimal("100.00"), LocalDate.of(2026, 9, 1),
                null, null, "Investimentos", "OUTROS", "VARIAVEL", null, null,
                "INVESTIMENTO");

        assertThatThrownBy(() -> request.paraDominio(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("receita não possui categoria de despesa");
    }
}

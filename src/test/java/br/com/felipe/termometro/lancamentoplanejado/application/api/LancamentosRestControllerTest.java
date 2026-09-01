package br.com.felipe.termometro.lancamentoplanejado.application.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.service.ConsultaLancamentosService;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LancamentosRestControllerTest {

    @Test
    void encaminhaFiltrosCombinadosEPaginacaoEExpoeResumoDoPeriodo() {
        ConsultaLancamentosService service = Mockito.mock(ConsultaLancamentosService.class);
        when(service.consulta(any())).thenReturn(new ConsultaLancamentosService.Resultado(
                List.of(), 61, Dinheiro.de("2300.00"), Dinheiro.de("5000.00"),
                Dinheiro.de("4500.00"), Dinheiro.de("2700.00"), 2,
                1, 30, true, Map.of()));
        LancamentosRestController controller = new LancamentosRestController(service);
        UUID conta = UUID.randomUUID();
        UUID cartao = UUID.randomUUID();

        var resposta = controller.consulta("2026-08", "DESPESA", "ATRASADO", conta,
                cartao, "Casa", "aluguel", 1, 30);

        ArgumentCaptor<ConsultaLancamentosService.Filtro> captor =
                ArgumentCaptor.forClass(ConsultaLancamentosService.Filtro.class);
        verify(service).consulta(captor.capture());
        assertThat(captor.getValue()).satisfies(filtro -> {
            assertThat(filtro.competencia().valor().toString()).isEqualTo("2026-08");
            assertThat(filtro.tipo()).isEqualTo("DESPESA");
            assertThat(filtro.status()).isEqualTo("ATRASADO");
            assertThat(filtro.contaId()).isEqualTo(conta);
            assertThat(filtro.cartaoId()).isEqualTo(cartao);
            assertThat(filtro.categoria()).isEqualTo("Casa");
            assertThat(filtro.texto()).isEqualTo("aluguel");
            assertThat(filtro.pagina()).isEqualTo(1);
        });
        assertThat(resposta.saldoRealizado().valor()).isEqualByComparingTo("4500.00");
        assertThat(resposta.saldoPrevisto().valor()).isEqualByComparingTo("2700.00");
        assertThat(resposta.quantidadeAtrasados()).isEqualTo(2);
        assertThat(resposta.temMais()).isTrue();
    }
}

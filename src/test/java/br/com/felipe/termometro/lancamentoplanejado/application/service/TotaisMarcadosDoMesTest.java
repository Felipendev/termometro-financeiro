package br.com.felipe.termometro.lancamentoplanejado.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TotaisMarcadosDoMesTest {

    @Test
    void somaSomenteMarcacoesAtivasDaCompetencia() {
        LancamentoPlanejadoRepository repository = mock(LancamentoPlanejadoRepository.class);
        Competencia competencia = Competencia.parse("2026-08");
        when(repository.buscaPorCompetencia(competencia)).thenReturn(List.of(
                despesa("2200", LocalDate.of(2026, 8, 25), StatusLancamentoPlanejado.LIQUIDADO, MarcacaoPlanejamento.CUSTO_FIXO),
                despesa("500", LocalDate.of(2026, 8, 26), StatusLancamentoPlanejado.PENDENTE, MarcacaoPlanejamento.PISO_HUMANO),
                despesa("999", LocalDate.of(2026, 8, 20), StatusLancamentoPlanejado.CANCELADO, MarcacaoPlanejamento.CUSTO_FIXO)));

        TotaisMarcadosDoMes totais = new TotaisMarcadosDoMes(repository);

        assertThat(totais.total(competencia, MarcacaoPlanejamento.CUSTO_FIXO))
                .isEqualTo(Dinheiro.de("2200"));
        assertThat(totais.total(competencia, MarcacaoPlanejamento.PISO_HUMANO))
                .isEqualTo(Dinheiro.de("500"));
        verify(repository, times(2)).buscaPorCompetencia(competencia);
    }

    private static LancamentoPlanejado despesa(String valor, LocalDate data,
            StatusLancamentoPlanejado status, MarcacaoPlanejamento marcacao) {
        return new LancamentoPlanejado(UUID.randomUUID(), "item", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de(valor), data, status, null, null, null, null, null, marcacao);
    }
}

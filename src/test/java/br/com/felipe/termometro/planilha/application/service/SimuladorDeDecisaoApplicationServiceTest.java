package br.com.felipe.termometro.planilha.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.planilha.domain.DiaDaPlanilha;
import br.com.felipe.termometro.planilha.domain.FormaPagamento;
import br.com.felipe.termometro.planilha.domain.ItemDoDia;
import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import br.com.felipe.termometro.planilha.domain.TipoItemDoDia;
import br.com.felipe.termometro.planoajuste.application.service.PlanoAjusteService;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimuladorDeDecisaoApplicationServiceTest {

    private final PlanilhaService planilhaService = mock(PlanilhaService.class);
    private final LancamentoPlanejadoRepository lancamentoPlanejadoRepository = mock(LancamentoPlanejadoRepository.class);
    private final PlanoAjusteService planoAjusteService = mock(PlanoAjusteService.class);
    private SimuladorDeDecisaoApplicationService service;

    @BeforeEach
    void configura() {
        service = new SimuladorDeDecisaoApplicationService(planilhaService, lancamentoPlanejadoRepository, planoAjusteService);
    }

    @Test
    void confirmaCriaUmLancamentoPorParcelaComVencimentoMensalERateioExato() {
        ComandoDeDecisao decisao = new ComandoDeDecisao(
                LocalDate.of(2026, 9, 14), Dinheiro.de("500.00"), "Curso",
                FormaPagamento.CREDITO_PARCELADO, 3);

        List<java.util.UUID> gerados = service.confirma(decisao);

        assertThat(gerados).hasSize(3);
        verify(lancamentoPlanejadoRepository, times(3)).salva(any(LancamentoPlanejado.class));
    }

    @Test
    void confirmaCriaUmUnicoLancamentoQuandoNaoEParcelado() {
        ComandoDeDecisao decisao = new ComandoDeDecisao(
                LocalDate.of(2026, 9, 14), Dinheiro.de("120.00"), "Farmácia",
                FormaPagamento.DEBITO, 1);

        List<java.util.UUID> gerados = service.confirma(decisao);

        assertThat(gerados).hasSize(1);
        verify(lancamentoPlanejadoRepository, times(1)).salva(any(LancamentoPlanejado.class));
    }

    @Test
    void naoPropoePriorizacaoQuandoNenhumMesFechaNegativo() {
        Competencia setembro = Competencia.de(2026, 9);
        PlanilhaDoMes mesPositivo = mesComSaldoFinal(setembro, Dinheiro.de("500"));
        when(planilhaService.consulta(setembro)).thenReturn(mesPositivo);
        when(planilhaService.consultaComItensExtras(org.mockito.ArgumentMatchers.eq(setembro), any())).thenReturn(mesPositivo);

        ComandoDeDecisao decisao = new ComandoDeDecisao(
                LocalDate.of(2026, 9, 14), Dinheiro.de("100"), "Compra", FormaPagamento.DEBITO, 1);

        ResultadoDaSimulacao resultado = service.simula(decisao, setembro, setembro);

        assertThat(resultado.priorizacaoSeDeficit()).isNull();
        verify(planoAjusteService, never()).gera(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void propoePriorizacaoQuandoAlgumMesFechaNegativo() {
        Competencia setembro = Competencia.de(2026, 9);
        PlanilhaDoMes mesNegativo = mesComSaldoFinal(setembro, Dinheiro.de("-50"));
        when(planilhaService.consulta(setembro)).thenReturn(mesNegativo);
        when(planilhaService.consultaComItensExtras(org.mockito.ArgumentMatchers.eq(setembro), any())).thenReturn(mesNegativo);
        when(planoAjusteService.gera(any(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(mock(PlanoDeAjuste.class));

        ComandoDeDecisao decisao = new ComandoDeDecisao(
                LocalDate.of(2026, 9, 14), Dinheiro.de("1000"), "Compra grande", FormaPagamento.CREDITO_AVISTA, 1);

        ResultadoDaSimulacao resultado = service.simula(decisao, setembro, setembro);

        assertThat(resultado.priorizacaoSeDeficit()).isNotNull();
    }

    private PlanilhaDoMes mesComSaldoFinal(Competencia competencia, Dinheiro saldo) {
        DiaDaPlanilha dia = new DiaDaPlanilha(competencia.ultimoDia(), DayOfWeek.MONDAY,
                List.of(new ItemDoDia("saldo simulado", saldo.absoluto(),
                        saldo.ehNegativo() ? TipoItemDoDia.SAIDA : TipoItemDoDia.ENTRADA, "MANUAL")),
                Dinheiro.ZERO, false, saldo, null);
        return PlanilhaDoMes.de(competencia, List.of(dia));
    }
}

package br.com.felipe.termometro.planilha.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.EscopoEdicaoRecorrencia;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.planilha.application.repository.DiarioOverrideRepository;
import br.com.felipe.termometro.planilha.application.repository.ObservacaoDoDiaRepository;
import br.com.felipe.termometro.planilha.application.repository.SaldoInicialRepository;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlanilhaApplicationServiceEdicaoTest {

    @Test
    void editaLiquidadoReabrindoEliquidandoNovamente() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoPlanejadoApplicationService lancamentos = mock(LancamentoPlanejadoApplicationService.class);
        UUID id = UUID.randomUUID();
        var existente = new LancamentoPlanejado(
                id, "Mercado antigo", TipoLancamentoPlanejado.DESPESA, Dinheiro.de("100"),
                LocalDate.of(2026, 9, 3), StatusLancamentoPlanejado.LIQUIDADO);
        var categoria = new CategoriaDoLancamento("Mercado", "ALIMENTACAO", "VARIAVEL");
        var alteracoes = new LancamentoPlanejado(
                id, "Mercado correto", TipoLancamentoPlanejado.DESPESA, Dinheiro.de("125.90"),
                LocalDate.of(2026, 9, 4), StatusLancamentoPlanejado.PENDENTE,
                null, null, categoria, null, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(existente));
        when(lancamentos.edita(any(), any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(lancamentos.liquidar(id)).thenReturn(alteracoes.liquidar());

        PlanilhaApplicationService service = new PlanilhaApplicationService(
                planejados, mock(LancamentoImportadoRepository.class),
                mock(DiarioOverrideRepository.class), mock(ObservacaoDoDiaRepository.class),
                mock(SaldoInicialRepository.class), lancamentos,
                Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC));

        service.editaLancamento(alteracoes, EscopoEdicaoRecorrencia.ESTA);

        var ordem = inOrder(lancamentos);
        ordem.verify(lancamentos).reabrir(id);
        ArgumentCaptor<LancamentoPlanejado> salvo = ArgumentCaptor.forClass(LancamentoPlanejado.class);
        ordem.verify(lancamentos).edita(salvo.capture(), eq(EscopoEdicaoRecorrencia.ESTA));
        ordem.verify(lancamentos).liquidar(id);
        assertThat(salvo.getValue().descricao()).isEqualTo("Mercado correto");
        assertThat(salvo.getValue().valor()).isEqualTo(Dinheiro.de("125.90"));
        assertThat(salvo.getValue().categoria()).isEqualTo(categoria);
    }

    @Test
    void removeLiquidadoSomenteDepoisDeReabrir() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoPlanejadoApplicationService lancamentos = mock(LancamentoPlanejadoApplicationService.class);
        UUID id = UUID.randomUUID();
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(new LancamentoPlanejado(
                id, "Conta", TipoLancamentoPlanejado.DESPESA, Dinheiro.de("90"),
                LocalDate.of(2026, 9, 4), StatusLancamentoPlanejado.LIQUIDADO)));
        PlanilhaApplicationService service = new PlanilhaApplicationService(
                planejados, mock(LancamentoImportadoRepository.class),
                mock(DiarioOverrideRepository.class), mock(ObservacaoDoDiaRepository.class),
                mock(SaldoInicialRepository.class), lancamentos,
                Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC));

        service.removeLancamento(id);

        var ordem = inOrder(lancamentos);
        ordem.verify(lancamentos).reabrir(id);
        ordem.verify(lancamentos).remove(id);
        verify(planejados).buscaPorId(id);
    }
}

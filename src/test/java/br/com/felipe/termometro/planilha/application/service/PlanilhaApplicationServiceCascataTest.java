package br.com.felipe.termometro.planilha.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.planilha.application.repository.DiarioOverrideRepository;
import br.com.felipe.termometro.planilha.application.repository.ObservacaoDoDiaRepository;
import br.com.felipe.termometro.planilha.application.repository.SaldoInicialRepository;
import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanilhaApplicationServiceCascataTest {

    private static final Clock RELOGIO = Clock.fixed(
            Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void usaEntradasESaidasCadastradasSemExigirSaldoManual() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoImportadoRepository importados = mock(LancamentoImportadoRepository.class);
        DiarioOverrideRepository diarios = mock(DiarioOverrideRepository.class);
        ObservacaoDoDiaRepository observacoes = mock(ObservacaoDoDiaRepository.class);
        SaldoInicialRepository saldoInicial = mock(SaldoInicialRepository.class);
        var receitaSetembro = lancamento("Salário", TipoLancamentoPlanejado.RECEITA,
                "100", LocalDate.of(2026, 9, 5));
        var despesaOutubro = lancamento("Mercado", TipoLancamentoPlanejado.DESPESA,
                "30", LocalDate.of(2026, 10, 2));
        when(planejados.primeiraData()).thenReturn(Optional.of(LocalDate.of(2026, 9, 5)));
        when(planejados.buscaPorCompetencia(any())).thenAnswer(invocacao -> {
            Competencia competencia = invocacao.getArgument(0);
            if (competencia.equals(Competencia.parse("2026-09"))) return List.of(receitaSetembro);
            if (competencia.equals(Competencia.parse("2026-10"))) return List.of(despesaOutubro);
            return List.of();
        });
        when(importados.primeiraData()).thenReturn(Optional.empty());
        when(importados.buscaPorCompetencia(any())).thenReturn(List.of());
        when(diarios.primeiraData()).thenReturn(Optional.empty());
        when(diarios.buscaEntre(any(), any())).thenReturn(Map.of());
        when(observacoes.buscaEntre(any(), any())).thenReturn(Map.of());
        when(saldoInicial.busca()).thenReturn(Optional.empty());

        var service = new PlanilhaApplicationService(planejados, importados, diarios, observacoes,
                saldoInicial, mock(LancamentoPlanejadoApplicationService.class), RELOGIO);

        var outubro = service.consulta(Competencia.parse("2026-10"));

        assertThat(outubro.dias().getFirst().saldo()).isEqualTo(Dinheiro.de("100"));
        assertThat(outubro.saldoFinal()).isEqualTo(Dinheiro.de("70"));
    }

    @Test
    void saldoManualDentroDoMesNaoBloqueiaAConsultaDoProprioMes() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoImportadoRepository importados = mock(LancamentoImportadoRepository.class);
        DiarioOverrideRepository diarios = mock(DiarioOverrideRepository.class);
        ObservacaoDoDiaRepository observacoes = mock(ObservacaoDoDiaRepository.class);
        SaldoInicialRepository saldoInicial = mock(SaldoInicialRepository.class);
        var receita = lancamento("Salário", TipoLancamentoPlanejado.RECEITA,
                "100", LocalDate.of(2026, 9, 5));
        when(planejados.primeiraData()).thenReturn(Optional.of(LocalDate.of(2026, 9, 5)));
        when(planejados.buscaPorCompetencia(any())).thenReturn(List.of(receita));
        when(importados.primeiraData()).thenReturn(Optional.empty());
        when(importados.buscaPorCompetencia(any())).thenReturn(List.of());
        when(diarios.primeiraData()).thenReturn(Optional.empty());
        when(diarios.buscaEntre(any(), any())).thenReturn(Map.of());
        when(observacoes.buscaEntre(any(), any())).thenReturn(Map.of());
        when(saldoInicial.busca()).thenReturn(Optional.of(
                new SaldoInicialPlanilha(LocalDate.of(2026, 9, 15), Dinheiro.de("500"))));

        var service = new PlanilhaApplicationService(planejados, importados, diarios, observacoes,
                saldoInicial, mock(LancamentoPlanejadoApplicationService.class), RELOGIO);

        var setembro = service.consulta(Competencia.parse("2026-09"));

        assertThat(setembro.saldoFinal()).isEqualTo(Dinheiro.de("100"));
    }

    @Test
    void repassaSaldoPositivoENegativoEntreMesesSemZerar() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoImportadoRepository importados = mock(LancamentoImportadoRepository.class);
        DiarioOverrideRepository diarios = mock(DiarioOverrideRepository.class);
        ObservacaoDoDiaRepository observacoes = mock(ObservacaoDoDiaRepository.class);
        SaldoInicialRepository saldoInicial = mock(SaldoInicialRepository.class);

        var receitaSetembro = lancamento("Salário", TipoLancamentoPlanejado.RECEITA,
                "50", LocalDate.of(2026, 9, 5));
        var despesaOutubro = lancamento("Conta extraordinária", TipoLancamentoPlanejado.DESPESA,
                "175", LocalDate.of(2026, 10, 2));
        when(planejados.buscaPorCompetencia(any())).thenAnswer(invocacao -> {
            Competencia competencia = invocacao.getArgument(0);
            if (competencia.equals(Competencia.parse("2026-09"))) return List.of(receitaSetembro);
            if (competencia.equals(Competencia.parse("2026-10"))) return List.of(despesaOutubro);
            return List.of();
        });
        when(importados.buscaPorCompetencia(any())).thenReturn(List.of());
        when(diarios.buscaEntre(any(), any())).thenReturn(Map.of());
        when(observacoes.buscaEntre(any(), any())).thenReturn(Map.of());
        when(saldoInicial.busca()).thenReturn(Optional.of(
                new SaldoInicialPlanilha(LocalDate.of(2026, 8, 31), Dinheiro.de("100"))));

        var service = new PlanilhaApplicationService(planejados, importados, diarios, observacoes,
                saldoInicial, mock(LancamentoPlanejadoApplicationService.class), RELOGIO);

        var setembro = service.consulta(Competencia.parse("2026-09"));
        var outubro = service.consulta(Competencia.parse("2026-10"));

        assertThat(setembro.saldoFinal()).isEqualTo(Dinheiro.de("150"));
        assertThat(outubro.dias().getFirst().saldo()).isEqualTo(Dinheiro.de("150"));
        assertThat(outubro.saldoFinal()).isEqualTo(Dinheiro.de("-25"));
    }

    @Test
    void consomeSaldoDoMesAnteriorNoDiarioEApenasDepoisSomaOSalario() {
        LancamentoPlanejadoRepository planejados = mock(LancamentoPlanejadoRepository.class);
        LancamentoImportadoRepository importados = mock(LancamentoImportadoRepository.class);
        DiarioOverrideRepository diarios = mock(DiarioOverrideRepository.class);
        ObservacaoDoDiaRepository observacoes = mock(ObservacaoDoDiaRepository.class);
        SaldoInicialRepository saldoInicial = mock(SaldoInicialRepository.class);

        var sobraDeSetembro = lancamento("Sobra", TipoLancamentoPlanejado.RECEITA,
                "10", LocalDate.of(2026, 9, 30));
        var salarioDeOutubro = lancamento("Salário", TipoLancamentoPlanejado.RECEITA,
                "10000", LocalDate.of(2026, 10, 5));
        when(planejados.buscaPorCompetencia(any())).thenAnswer(invocacao -> {
            Competencia competencia = invocacao.getArgument(0);
            if (competencia.equals(Competencia.parse("2026-09"))) return List.of(sobraDeSetembro);
            if (competencia.equals(Competencia.parse("2026-10"))) return List.of(salarioDeOutubro);
            return List.of();
        });
        when(importados.buscaPorCompetencia(any())).thenReturn(List.of());
        when(diarios.buscaEntre(any(), any())).thenReturn(Map.of(
                LocalDate.of(2026, 10, 1), Dinheiro.de("2"),
                LocalDate.of(2026, 10, 2), Dinheiro.de("2"),
                LocalDate.of(2026, 10, 3), Dinheiro.de("2"),
                LocalDate.of(2026, 10, 4), Dinheiro.de("2"),
                LocalDate.of(2026, 10, 5), Dinheiro.de("2")));
        when(observacoes.buscaEntre(any(), any())).thenReturn(Map.of());
        when(saldoInicial.busca()).thenReturn(Optional.empty());

        var service = new PlanilhaApplicationService(planejados, importados, diarios, observacoes,
                saldoInicial, mock(LancamentoPlanejadoApplicationService.class), RELOGIO);

        var outubro = service.consulta(Competencia.parse("2026-10"));

        assertThat(outubro.dias().get(0).saldo()).isEqualTo(Dinheiro.de("8"));
        assertThat(outubro.dias().get(1).saldo()).isEqualTo(Dinheiro.de("6"));
        assertThat(outubro.dias().get(2).saldo()).isEqualTo(Dinheiro.de("4"));
        assertThat(outubro.dias().get(3).saldo()).isEqualTo(Dinheiro.de("2"));
        assertThat(outubro.dias().get(4).saldo()).isEqualTo(Dinheiro.de("10000"));
    }

    private LancamentoPlanejado lancamento(String descricao, TipoLancamentoPlanejado tipo,
            String valor, LocalDate data) {
        return new LancamentoPlanejado(UUID.randomUUID(), descricao, tipo, Dinheiro.de(valor), data,
                StatusLancamentoPlanejado.LIQUIDADO);
    }
}

package br.com.felipe.termometro.lancamentoplanejado.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository.LancamentoImportado;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsultaLancamentosServiceTest {

    @Test
    void exigeCompetenciaParaNaoCarregarTodoOHistorico() {
        LancamentoPlanejadoRepository repository = Mockito.mock(LancamentoPlanejadoRepository.class);

        assertThatThrownBy(() -> new ConsultaLancamentosService(repository).consulta(
                new ConsultaLancamentosService.Filtro(null, null, null,
                        null, null, null, null, 0, 30)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("competencia");

        verifyNoInteractions(repository);
    }

    @Test
    void combinaCompetenciaTipoStatusContaCategoriaETextoAntesDePaginar() {
        LancamentoPlanejadoRepository repository = Mockito.mock(LancamentoPlanejadoRepository.class);
        UUID conta = UUID.randomUUID();
        LancamentoPlanejado mercado = item("Mercado do bairro", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 8, 12), conta, "Mercado");
        LancamentoPlanejado aluguel = item("Aluguel", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 8, 5), conta, "Casa");
        LancamentoPlanejado setembro = item("Mercado setembro", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 9, 2), conta, "Mercado");
        when(repository.buscaPorCompetencia(Competencia.parse("2026-08"))).thenReturn(List.of(mercado, aluguel));

        ConsultaLancamentosService.Resultado resultado = new ConsultaLancamentosService(repository).consulta(
                new ConsultaLancamentosService.Filtro(Competencia.parse("2026-08"), "DESPESA", "PENDENTE",
                        conta, null, "Mercado", "bairro", 0, 20));

        assertThat(resultado.itens()).containsExactly(mercado);
        assertThat(resultado.totalDeItens()).isEqualTo(1);
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("125.50");
    }

    @Test
    void resumeRealizadoPrevistoAtrasosEPaginacaoSemContarTransferenciasOuCancelados() {
        LancamentoPlanejadoRepository repository = Mockito.mock(LancamentoPlanejadoRepository.class);
        UUID conta = UUID.randomUUID();
        LancamentoPlanejado receitaRealizada = item("Salário", TipoLancamentoPlanejado.RECEITA,
                StatusLancamentoPlanejado.LIQUIDADO, LocalDate.of(2026, 8, 5), conta, "Salário", "5000.00");
        LancamentoPlanejado despesaRealizada = item("Mercado", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.LIQUIDADO, LocalDate.of(2026, 8, 6), conta, "Mercado", "500.00");
        LancamentoPlanejado despesaAtrasada = item("Aluguel", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 8, 20), conta, "Casa", "2000.00");
        LancamentoPlanejado transferencia = item("Reserva", TipoLancamentoPlanejado.TRANSFERENCIA,
                StatusLancamentoPlanejado.LIQUIDADO, LocalDate.of(2026, 8, 7), conta, "Outros", "900.00");
        LancamentoPlanejado cancelada = item("Compra cancelada", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.CANCELADO, LocalDate.of(2026, 8, 8), conta, "Compras", "300.00");
        when(repository.buscaPorCompetencia(Competencia.parse("2026-08"))).thenReturn(List.of(receitaRealizada, despesaRealizada,
                despesaAtrasada, transferencia, cancelada));
        Clock relogio = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneId.of("America/Bahia"));

        ConsultaLancamentosService.Resultado resultado = new ConsultaLancamentosService(repository, relogio)
                .consulta(new ConsultaLancamentosService.Filtro(Competencia.parse("2026-08"), null, null,
                        null, null, null, null, 0, 2));

        assertThat(resultado.itens()).hasSize(2);
        assertThat(resultado.totalDeItens()).isEqualTo(5);
        assertThat(resultado.totalDespesas().valor()).isEqualByComparingTo("2500.00");
        assertThat(resultado.totalReceitas().valor()).isEqualByComparingTo("5000.00");
        assertThat(resultado.saldoRealizado().valor()).isEqualByComparingTo("4500.00");
        assertThat(resultado.saldoPrevisto().valor()).isEqualByComparingTo("2500.00");
        assertThat(resultado.quantidadeAtrasados()).isEqualTo(1);
        assertThat(resultado.pagina()).isZero();
        assertThat(resultado.tamanho()).isEqualTo(2);
        assertThat(resultado.temMais()).isTrue();
    }

    @Test
    void filtraAtrasadosComoStatusVisual() {
        LancamentoPlanejadoRepository repository = Mockito.mock(LancamentoPlanejadoRepository.class);
        UUID conta = UUID.randomUUID();
        LancamentoPlanejado atrasado = item("Conta vencida", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 8, 20), conta, "Casa");
        LancamentoPlanejado futuro = item("Conta futura", TipoLancamentoPlanejado.DESPESA,
                StatusLancamentoPlanejado.PENDENTE, LocalDate.of(2026, 8, 30), conta, "Casa");
        when(repository.buscaPorCompetencia(Competencia.parse("2026-08"))).thenReturn(List.of(atrasado, futuro));
        Clock relogio = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneId.of("America/Bahia"));

        ConsultaLancamentosService.Resultado resultado = new ConsultaLancamentosService(repository, relogio)
                .consulta(new ConsultaLancamentosService.Filtro(Competencia.parse("2026-08"), null,
                        "ATRASADO", null, null, null, null, 0, 20));

        assertThat(resultado.itens()).containsExactly(atrasado);
    }

    @Test
    void incluiTransacoesImportadasComoRealizadasENaoEditaveisSemDuplicarMovimentoManual() {
        LancamentoPlanejadoRepository repository = Mockito.mock(LancamentoPlanejadoRepository.class);
        LancamentoImportadoRepository importados = Mockito.mock(LancamentoImportadoRepository.class);
        UUID idImportado = UUID.randomUUID();
        when(repository.buscaPorCompetencia(Competencia.parse("2026-08"))).thenReturn(List.of());
        when(importados.buscaPorCompetencia(Competencia.parse("2026-08"))).thenReturn(List.of(
                new LancamentoImportado(idImportado, "Farmácia", Dinheiro.de("-89.90"),
                        LocalDate.of(2026, 8, 21), "Nubank", "Saúde", "SAUDE", "VARIAVEL", "CSV"),
                new LancamentoImportado(UUID.randomUUID(), "Pix entre contas", Dinheiro.de("-50.00"),
                        LocalDate.of(2026, 8, 20), "Nubank", "TRANSFERENCIA_PESSOAL",
                        "TRANSFERENCIA", "NAO_E_GASTO", "CSV")));
        Clock relogio = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneId.of("America/Bahia"));

        ConsultaLancamentosService.Resultado resultado = new ConsultaLancamentosService(
                repository, importados, relogio).consulta(new ConsultaLancamentosService.Filtro(
                        Competencia.parse("2026-08"), "DESPESA", "LIQUIDADO", null, null,
                        "Saúde", "farm", 0, 20));

        assertThat(resultado.itens()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(idImportado);
            assertThat(item.transacaoId()).isEqualTo(idImportado);
            assertThat(item.valor().valor()).isEqualByComparingTo("89.90");
        });
        assertThat(resultado.metadados().get(idImportado).editavel()).isFalse();
        assertThat(resultado.metadados().get(idImportado).contaOuCartao()).isEqualTo("Nubank");
        assertThat(resultado.saldoRealizado().valor()).isEqualByComparingTo("-89.90");

        ConsultaLancamentosService.Resultado completo = new ConsultaLancamentosService(
                repository, importados, relogio).consulta(new ConsultaLancamentosService.Filtro(
                        Competencia.parse("2026-08"), null, null, null, null,
                        null, null, 0, 20));
        assertThat(completo.itens()).filteredOn(item -> item.descricao().equals("Pix entre contas"))
                .singleElement().extracting(LancamentoPlanejado::tipo)
                .isEqualTo(TipoLancamentoPlanejado.TRANSFERENCIA);
        assertThat(completo.totalDespesas().valor()).isEqualByComparingTo("89.90");
    }

    private static LancamentoPlanejado item(String descricao, TipoLancamentoPlanejado tipo,
                                             StatusLancamentoPlanejado status, LocalDate data,
                                             UUID conta, String categoria) {
        return item(descricao, tipo, status, data, conta, categoria, "125.50");
    }

    private static LancamentoPlanejado item(String descricao, TipoLancamentoPlanejado tipo,
                                             StatusLancamentoPlanejado status, LocalDate data,
                                             UUID conta, String categoria, String valor) {
        UUID destino = tipo == TipoLancamentoPlanejado.TRANSFERENCIA ? UUID.randomUUID() : null;
        return new LancamentoPlanejado(UUID.randomUUID(), descricao, tipo, Dinheiro.de(valor), data, status,
                conta, destino, new CategoriaDoLancamento(categoria, "ALIMENTACAO", "VARIAVEL"), null, null);
    }
}

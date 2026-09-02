package br.com.felipe.termometro.lancamentoplanejado.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.felipe.termometro.classificacao.application.repository.RegraDeCategorizacaoRepository;
import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.GrupoDeCategoria;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.application.service.ImportacaoProcessadaService;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.contamanual.application.repository.ContaManualRepository;
import br.com.felipe.termometro.contamanual.domain.ContaManual;
import br.com.felipe.termometro.contamanual.domain.TipoContaManual;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.recorrencia.RecorrenciaLancamentoService;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LancamentoPlanejadoApplicationServiceTest {

    @Test
    void liquidarDespesaRegistraSaidaManualEEncerraPendencia() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        LancamentoPlanejado pendente = item(id, TipoLancamentoPlanejado.DESPESA, null, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(pendente));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(transacoes.salvaTodasDoLancamentoPlanejado(eq(id), eq("manual-planejado"), any()))
                .thenAnswer(invocacao -> invocacao.getArgument(2));

        LancamentoPlanejado resultado = new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).liquidar(id);

        ArgumentCaptor<List<TransacaoBruta>> movimentos = ArgumentCaptor.forClass(List.class);
        verify(transacoes).salvaTodasDoLancamentoPlanejado(
                eq(id), eq("manual-planejado"), movimentos.capture());
        assertThat(movimentos.getValue()).singleElement().satisfies(movimento -> {
            assertThat(movimento.valor()).isEqualTo(Dinheiro.de("125.50").negado());
            assertThat(movimento.descricao()).isEqualTo("Aluguel");
            assertThat(movimento.eventoId()).isNull();
        });
        assertThat(resultado.status()).isEqualTo(StatusLancamentoPlanejado.LIQUIDADO);
        verify(planejados).salva(resultado);
        verify(processamento).processa(movimentos.getValue());
    }

    @Test
    void liquidarTransferenciaRegistraUmaSaidaEUmaEntradaSemDuplicarDespesa() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        UUID origem = UUID.randomUUID();
        UUID destino = UUID.randomUUID();
        LancamentoPlanejado pendente = item(id, TipoLancamentoPlanejado.TRANSFERENCIA, origem, destino);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(pendente));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        ContaManual contaOrigem = conta(origem, "Origem", "500");
        ContaManual contaDestino = conta(destino, "Destino", "100");
        when(contas.buscaPorId(origem)).thenReturn(Optional.of(contaOrigem));
        when(contas.buscaPorId(destino)).thenReturn(Optional.of(contaDestino));

        new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).liquidar(id);

        ArgumentCaptor<List<TransacaoBruta>> movimentos = ArgumentCaptor.forClass(List.class);
        verify(transacoes).salvaTodasDoLancamentoPlanejado(
                eq(id), eq("manual-conta-" + origem), movimentos.capture());
        verify(transacoes).salvaTodasDoLancamentoPlanejado(
                eq(id), eq("manual-conta-" + destino), movimentos.capture());
        assertThat(movimentos.getAllValues()).extracting(lista -> lista.getFirst().valor())
                .containsExactly(Dinheiro.de("125.50").negado(), Dinheiro.de("125.50"));
        verify(processamento).processa(List.of());
        verify(contas).salva(contaOrigem.debitar(Dinheiro.de("125.50")));
        verify(contas).salva(contaDestino.creditar(Dinheiro.de("125.50")));
    }

    @Test
    void liquidarDespesaComCategoriaAprendeRegraAntesDeEnviarParaAnalise() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        CategoriaDoLancamento categoria = new CategoriaDoLancamento("Mercado", "ALIMENTACAO", "VARIAVEL");
        LancamentoPlanejado pendente = new LancamentoPlanejado(id, "Feira do bairro", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("125.50"), LocalDate.of(2026, 8, 28), StatusLancamentoPlanejado.PENDENTE,
                null, null, categoria, null, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(pendente));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(transacoes.salvaTodasDoLancamentoPlanejado(eq(id), eq("manual-planejado"), any()))
                .thenAnswer(invocacao -> invocacao.getArgument(2));

        new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).liquidar(id);

        verify(regras).aprende("FEIRA DO BAIRRO", new Categoria("Mercado", GrupoDeCategoria.ALIMENTACAO, Natureza.VARIAVEL));
        verify(processamento).processa(any());
    }

    @Test
    void reabrirDespesaIgnoraSomenteMovimentosManuaisDoLancamentoEEstornaConta() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        LancamentoPlanejado liquidado = new LancamentoPlanejado(id, "Aluguel", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("125.50"), LocalDate.of(2026, 8, 28), StatusLancamentoPlanejado.LIQUIDADO,
                contaId, null);
        ContaManual conta = conta(contaId, "Conta", "500");
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(liquidado));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(contas.buscaPorId(contaId)).thenReturn(Optional.of(conta));

        LancamentoPlanejado resultado = new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).reabrir(id);

        verify(transacoes).ignoraMovimentosDoLancamentoPlanejado(id);
        verify(contas).salva(conta.creditar(Dinheiro.de("125.50")));
        assertThat(resultado.status()).isEqualTo(StatusLancamentoPlanejado.PENDENTE);
    }

    @Test
    void liquidarEReabrirSaoIdempotentes() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        LancamentoPlanejado liquidado = item(id, TipoLancamentoPlanejado.DESPESA, null, null).liquidar();
        LancamentoPlanejado pendente = item(id, TipoLancamentoPlanejado.DESPESA, null, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(liquidado), Optional.of(pendente));
        LancamentoPlanejadoApplicationService service = new LancamentoPlanejadoApplicationService(
                planejados, transacoes, processamento, contas, regras, recorrencia);

        assertThat(service.liquidar(id)).isSameAs(liquidado);
        assertThat(service.reabrir(id)).isSameAs(pendente);
        verifyNoInteractions(transacoes, processamento, contas, regras);
    }

    @Test
    void reabrirTransferenciaEstornaOsDoisSaldos() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID(); UUID origem = UUID.randomUUID(); UUID destino = UUID.randomUUID();
        LancamentoPlanejado liquidado = item(id, TipoLancamentoPlanejado.TRANSFERENCIA, origem, destino).liquidar();
        ContaManual contaOrigem = conta(origem, "Origem", "374.50");
        ContaManual contaDestino = conta(destino, "Destino", "225.50");
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(liquidado));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(contas.buscaPorId(origem)).thenReturn(Optional.of(contaOrigem));
        when(contas.buscaPorId(destino)).thenReturn(Optional.of(contaDestino));

        new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).reabrir(id);

        verify(contas).salva(contaOrigem.creditar(Dinheiro.de("125.50")));
        verify(contas).salva(contaDestino.debitar(Dinheiro.de("125.50")));
    }

    @Test
    void cancelarPendenteApenasMudaEstadoESegundaChamadaEIdempotente() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        LancamentoPlanejado pendente = item(id, TipoLancamentoPlanejado.DESPESA, null, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(pendente));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        LancamentoPlanejado resultado = new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).cancelar(id);

        assertThat(resultado.status()).isEqualTo(StatusLancamentoPlanejado.CANCELADO);
        verify(planejados).salva(resultado);
    }

    @Test
    void editarPendentePreservaSeuEstadoEFormaDePagamento() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID(); UUID contaId = UUID.randomUUID();
        LancamentoPlanejado existente = item(id, TipoLancamentoPlanejado.DESPESA, contaId, null);
        LancamentoPlanejado edicao = new LancamentoPlanejado(id, "Aluguel revisado", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("200"), LocalDate.of(2026, 8, 29), StatusLancamentoPlanejado.PENDENTE, contaId, null);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(existente));
        when(planejados.salva(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        LancamentoPlanejado resultado = new LancamentoPlanejadoApplicationService(planejados, transacoes, processamento, contas, regras, recorrencia).edita(edicao);

        assertThat(resultado.descricao()).isEqualTo("Aluguel revisado");
        assertThat(resultado.status()).isEqualTo(StatusLancamentoPlanejado.PENDENTE);
        assertThat(resultado.contaOrigemId()).isEqualTo(contaId);
    }

    @Test
    void marcarLancamentoQueJaExistiaComoRecorrenteCriaASerie() {
        LancamentoPlanejadoRepository planejados = Mockito.mock(LancamentoPlanejadoRepository.class);
        TransacaoRepository transacoes = Mockito.mock(TransacaoRepository.class);
        ImportacaoProcessadaService processamento = Mockito.mock(ImportacaoProcessadaService.class);
        ContaManualRepository contas = Mockito.mock(ContaManualRepository.class);
        RegraDeCategorizacaoRepository regras = Mockito.mock(RegraDeCategorizacaoRepository.class);
        RecorrenciaLancamentoService recorrencia = Mockito.mock(RecorrenciaLancamentoService.class);
        UUID id = UUID.randomUUID();
        LancamentoPlanejado existente = item(id, TipoLancamentoPlanejado.DESPESA, null, null);
        LancamentoPlanejado edicaoComDia = new LancamentoPlanejado(id, "Aluguel", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("2200"), LocalDate.of(2026, 9, 10), StatusLancamentoPlanejado.PENDENTE,
                null, null, null, null, null, MarcacaoPlanejamento.CUSTO_FIXO, null, null, 10);
        when(planejados.buscaPorId(id)).thenReturn(Optional.of(existente));
        when(recorrencia.criaSerie(any(), eq(10))).thenAnswer(invocacao ->
                ((LancamentoPlanejado) invocacao.getArgument(0)).comRecorrencia(UUID.randomUUID(), 10));

        LancamentoPlanejado resultado = new LancamentoPlanejadoApplicationService(
                planejados, transacoes, processamento, contas, regras, recorrencia).edita(edicaoComDia);

        // sem isto, marcar "recorrente" num lançamento já existente só gravava o dia e nunca
        // gerava os meses seguintes — a série tem que nascer aqui também, não só na criação
        verify(recorrencia).criaSerie(any(), eq(10));
        assertThat(resultado.serieId()).isNotNull();
        verifyNoInteractions(transacoes);
    }

    private static LancamentoPlanejado item(UUID id, TipoLancamentoPlanejado tipo, UUID origem, UUID destino) {
        return new LancamentoPlanejado(id, "Aluguel", tipo, Dinheiro.de("125.50"), LocalDate.of(2026, 8, 28),
                StatusLancamentoPlanejado.PENDENTE, origem, destino);
    }

    private static ContaManual conta(UUID id, String nome, String saldo) {
        return new ContaManual(id, nome.toLowerCase(), nome, TipoContaManual.CORRENTE, Dinheiro.de(saldo), true);
    }
}

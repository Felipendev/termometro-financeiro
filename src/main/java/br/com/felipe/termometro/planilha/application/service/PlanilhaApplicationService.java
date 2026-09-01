package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository.LancamentoImportado;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.planilha.application.repository.DiarioOverrideRepository;
import br.com.felipe.termometro.planilha.application.repository.ObservacaoDoDiaRepository;
import br.com.felipe.termometro.planilha.application.repository.SaldoInicialRepository;
import br.com.felipe.termometro.planilha.domain.CalculadoraDeSaldoEmCascata;
import br.com.felipe.termometro.planilha.domain.DiaDaPlanilha;
import br.com.felipe.termometro.planilha.domain.ItemDoDia;
import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import br.com.felipe.termometro.planilha.domain.TipoItemDoDia;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composição pura (sem repositório de agregação próprio): reaproveita
 * {@link LancamentoPlanejadoRepository} e {@link LancamentoImportadoRepository} — as mesmas
 * portas que já fundem banco e manual para o {@code ConsultaLancamentosService} — só que
 * agrupando por dia em vez de por página, e cascateando o saldo mês após mês. A âncora manual
 * é opcional: sem uma âncora anterior ao mês corrente, a linha do tempo começa no mês atual
 * para que importações históricas não contaminem o caixa que o usuário passou a acompanhar agora.
 */
@Service
@RequiredArgsConstructor
public class PlanilhaApplicationService implements PlanilhaService {

    private final LancamentoPlanejadoRepository lancamentoPlanejadoRepository;
    private final LancamentoImportadoRepository lancamentoImportadoRepository;
    private final DiarioOverrideRepository diarioOverrideRepository;
    private final ObservacaoDoDiaRepository observacaoDoDiaRepository;
    private final SaldoInicialRepository saldoInicialRepository;
    private final LancamentoPlanejadoApplicationService lancamentoPlanejadoService;
    private final Clock relogio;

    @Override
    public PlanilhaDoMes consultaComItensExtras(Competencia competencia, Map<LocalDate, List<ItemDoDia>> itensExtras) {
        Optional<SaldoInicialPlanilha> saldoCadastrado = saldoInicialRepository.busca();
        Competencia competenciaAtual = Competencia.atual(relogio);
        Competencia inicioNatural = competencia.compareTo(competenciaAtual) >= 0
                ? competenciaAtual : competencia;
        boolean usaSaldoCadastrado = saldoCadastrado
                .map(saldo -> saldo.dataReferencia().isBefore(inicioNatural.primeiroDia()))
                .orElse(false);
        LocalDate inicioDoReplay = usaSaldoCadastrado
                ? saldoCadastrado.orElseThrow().dataReferencia().plusDays(1)
                : inicioNatural.primeiroDia();

        List<Competencia> competenciasDeApoio = Competencia.de(inicioDoReplay).ate(competencia).toList();

        Map<LocalDate, List<ItemDoDia>> lancamentosPorDia = new HashMap<>();
        for (Competencia competenciaDeApoio : competenciasDeApoio) {
            acumulaLancamentosPlanejados(competenciaDeApoio, lancamentosPorDia);
            acumulaLancamentosImportados(competenciaDeApoio, lancamentosPorDia);
        }
        itensExtras.forEach((data, itens) -> itens.forEach(item -> adiciona(lancamentosPorDia, data, item)));

        List<LocalDate> diasParaCalcular = competenciasDeApoio.stream()
                .flatMap(comp -> comp.primeiroDia().datesUntil(comp.ultimoDia().plusDays(1)))
                .filter(data -> !data.isBefore(inicioDoReplay))
                .sorted()
                .toList();

        Map<LocalDate, Dinheiro> diarios =
                diarioOverrideRepository.buscaEntre(inicioDoReplay, competencia.ultimoDia());
        Map<LocalDate, String> observacoes =
                observacaoDoDiaRepository.buscaEntre(inicioDoReplay, competencia.ultimoDia());
        Dinheiro valorInicial = usaSaldoCadastrado
                ? saldoCadastrado.orElseThrow().valor() : Dinheiro.ZERO;

        List<DiaDaPlanilha> todosOsDias = CalculadoraDeSaldoEmCascata.calcula(
                diasParaCalcular, lancamentosPorDia, diarios, observacoes, valorInicial);

        List<DiaDaPlanilha> diasDoMesPedido = todosOsDias.stream()
                .filter(dia -> competencia.contem(dia.data()))
                .toList();

        return PlanilhaDoMes.de(competencia, diasDoMesPedido);
    }

    @Override
    public SaldoInicialPlanilha defineSaldoInicial(LocalDate dataReferencia, Dinheiro valor) {
        return saldoInicialRepository.salva(new SaldoInicialPlanilha(dataReferencia, valor));
    }

    @Override
    public void sobrescreveDiario(LocalDate data, Dinheiro valor) {
        diarioOverrideRepository.salva(data, valor);
    }

    @Override
    public void sobrescreveDiarioEmSerie(LocalDate de, LocalDate ate, Dinheiro valor) {
        if (ate.isBefore(de)) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A data final não pode ser anterior à inicial.");
        }
        if (!Competencia.de(de).equals(Competencia.de(ate))) {
            throw APIException.build(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O preenchimento em série não pode cruzar a virada de mês.");
        }
        diarioOverrideRepository.salvaEmSerie(de, ate, valor);
    }

    @Override
    public void defineObservacao(LocalDate data, String texto) {
        observacaoDoDiaRepository.salva(data, texto);
    }

    private void acumulaLancamentosPlanejados(Competencia competencia,
            Map<LocalDate, List<ItemDoDia>> lancamentosPorDia) {
        for (LancamentoPlanejado item : lancamentoPlanejadoRepository.buscaPorCompetencia(competencia)) {
            if (item.status() == StatusLancamentoPlanejado.CANCELADO) {
                continue;
            }
            if (item.tipo() == TipoLancamentoPlanejado.TRANSFERENCIA) {
                continue;
            }
            TipoItemDoDia tipo = item.tipo() == TipoLancamentoPlanejado.RECEITA
                    ? TipoItemDoDia.ENTRADA : TipoItemDoDia.SAIDA;
            var categoria = item.categoria();
            adiciona(lancamentosPorDia, item.vencimento(),
                    new ItemDoDia(item.descricao(), item.valor(), tipo, "MANUAL", null,
                            item.id(), true,
                            categoria == null ? null : categoria.nome(),
                            categoria == null ? null : categoria.grupo(),
                            categoria == null ? null : categoria.natureza(),
                            item.origemReceita() == null ? null : item.origemReceita().name(),
                            item.marcacaoPlanejamento().name()));
        }
    }

    private void acumulaLancamentosImportados(Competencia competencia,
            Map<LocalDate, List<ItemDoDia>> lancamentosPorDia) {
        for (LancamentoImportado item : lancamentoImportadoRepository.buscaPorCompetencia(competencia)) {
            if (item.valorComSinal().ehZero()) {
                continue;
            }
            TipoItemDoDia tipo = item.valorComSinal().ehPositivo() ? TipoItemDoDia.ENTRADA : TipoItemDoDia.SAIDA;
            adiciona(lancamentosPorDia, item.data(),
                    new ItemDoDia(item.descricao(), item.valorComSinal().absoluto(), tipo, item.origem()));
        }
    }

    private void adiciona(Map<LocalDate, List<ItemDoDia>> lancamentosPorDia, LocalDate data, ItemDoDia item) {
        lancamentosPorDia.computeIfAbsent(data, chave -> new ArrayList<>()).add(item);
    }

    @Override
    @Transactional
    public LancamentoPlanejado adicionaLancamento(LancamentoPlanejado item) {
        return lancamentoPlanejadoService.salva(item);
    }

    @Override
    @Transactional
    public LancamentoPlanejado editaLancamento(LancamentoPlanejado alteracoes) {
        LancamentoPlanejado existente = lancamentoPlanejadoRepository.buscaPorId(alteracoes.id())
                .orElseThrow(() -> new IllegalArgumentException("lançamento não encontrado"));
        if (existente.tipo() == TipoLancamentoPlanejado.TRANSFERENCIA) {
            throw new IllegalStateException("transferências devem ser editadas na tela de lançamentos");
        }
        if (existente.tipo() != alteracoes.tipo()) {
            throw new IllegalArgumentException("o tipo do lançamento não pode ser alterado pela planilha");
        }
        boolean estavaLiquidado = existente.status() == StatusLancamentoPlanejado.LIQUIDADO;
        if (estavaLiquidado) {
            lancamentoPlanejadoService.reabrir(existente.id());
        }
        LancamentoPlanejado atualizado = new LancamentoPlanejado(
                existente.id(), alteracoes.descricao(), existente.tipo(), alteracoes.valor(),
                alteracoes.vencimento(), StatusLancamentoPlanejado.PENDENTE,
                existente.contaOrigemId(), existente.contaDestinoId(), alteracoes.categoria(),
                existente.cartaoManualId(), existente.transacaoId(),
                alteracoes.marcacaoPlanejamento(), alteracoes.origemReceita());
        LancamentoPlanejado salvo = lancamentoPlanejadoService.edita(atualizado);
        return estavaLiquidado ? lancamentoPlanejadoService.liquidar(salvo.id()) : salvo;
    }

    @Override
    @Transactional
    public void removeLancamento(java.util.UUID id) {
        LancamentoPlanejado existente = lancamentoPlanejadoRepository.buscaPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("lançamento não encontrado"));
        if (existente.tipo() == TipoLancamentoPlanejado.TRANSFERENCIA) {
            throw new IllegalStateException("transferências devem ser removidas na tela de lançamentos");
        }
        if (existente.status() == StatusLancamentoPlanejado.LIQUIDADO) {
            lancamentoPlanejadoService.reabrir(id);
        }
        lancamentoPlanejadoService.remove(id);
    }
}

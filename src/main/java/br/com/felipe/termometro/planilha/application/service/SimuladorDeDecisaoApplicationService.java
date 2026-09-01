package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.planilha.domain.FormaPagamento;
import br.com.felipe.termometro.planilha.domain.ItemDoDia;
import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import br.com.felipe.termometro.planilha.domain.TipoItemDoDia;
import br.com.felipe.termometro.planilha.domain.UsoDeCredito;
import br.com.felipe.termometro.planoajuste.application.service.PlanoAjusteService;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RN-23 — nenhuma tabela nova de "cenário": a simulação clona a leitura da planilha em memória
 * (RN-23.1) e só materializa em {@link LancamentoPlanejado} quando confirmada (RN-23.2), mesma
 * disciplina do resto do sistema (nada persiste antes de o Felipe confirmar).
 */
@Service
@RequiredArgsConstructor
public class SimuladorDeDecisaoApplicationService implements SimuladorDeDecisaoService {

    private static final int MESES_DA_RAMPA_PADRAO = 3;
    private static final BigDecimal FATOR_MAX_CORTE_PADRAO = BigDecimal.valueOf(0.35);

    private final PlanilhaService planilhaService;
    private final LancamentoPlanejadoRepository lancamentoPlanejadoRepository;
    private final PlanoAjusteService planoAjusteService;

    @Override
    public ResultadoDaSimulacao simula(ComandoDeDecisao decisao, Competencia de, Competencia ate) {
        Map<LocalDate, List<ItemDoDia>> itensExtras = geraItensExtras(decisao);
        List<Competencia> competencias = de.ate(ate).toList();

        List<PlanilhaDoMes> cenarioReal = competencias.stream().map(planilhaService::consulta).toList();
        List<PlanilhaDoMes> cenarioSimulado = competencias.stream()
                .map(competencia -> planilhaService.consultaComItensExtras(competencia, itensExtras))
                .toList();

        UsoDeCredito usoDeCreditoPrevisto = encontraClassificacaoDaPrimeiraParcela(cenarioSimulado, decisao);
        PlanoDeAjuste priorizacao = cenarioSimulado.stream()
                .filter(mes -> mes.saldoFinal().ehNegativo())
                .findFirst()
                .map(mes -> planoAjusteService.gera(mes.competencia(), MESES_DA_RAMPA_PADRAO, FATOR_MAX_CORTE_PADRAO))
                .orElse(null);

        return new ResultadoDaSimulacao(cenarioReal, cenarioSimulado, usoDeCreditoPrevisto, priorizacao);
    }

    @Override
    public List<UUID> confirma(ComandoDeDecisao decisao) {
        List<UUID> gerados = new ArrayList<>();
        for (Parcela parcela : geraParcelas(decisao)) {
            LancamentoPlanejado item = new LancamentoPlanejado(UUID.randomUUID(), parcela.descricao(),
                    TipoLancamentoPlanejado.DESPESA, parcela.valor(), parcela.vencimento(),
                    StatusLancamentoPlanejado.PENDENTE);
            lancamentoPlanejadoRepository.salva(item);
            gerados.add(item.id());
        }
        return List.copyOf(gerados);
    }

    private Map<LocalDate, List<ItemDoDia>> geraItensExtras(ComandoDeDecisao decisao) {
        String origem = decisao.formaPagamento() == FormaPagamento.DEBITO ? "MANUAL" : "SIMULACAO_CARTAO";
        Map<LocalDate, List<ItemDoDia>> itens = new HashMap<>();
        for (Parcela parcela : geraParcelas(decisao)) {
            ItemDoDia item = new ItemDoDia(parcela.descricao(), parcela.valor(), TipoItemDoDia.SAIDA, origem);
            itens.computeIfAbsent(parcela.vencimento(), chave -> new ArrayList<>()).add(item);
        }
        return itens;
    }

    private List<Parcela> geraParcelas(ComandoDeDecisao decisao) {
        if (decisao.formaPagamento() != FormaPagamento.CREDITO_PARCELADO || decisao.parcelas() == 1) {
            return List.of(new Parcela(decisao.data(), decisao.descricao(), decisao.valor()));
        }
        List<Dinheiro> valoresRateados = decisao.valor().ratear(decisao.parcelas());
        List<Parcela> parcelas = new ArrayList<>(decisao.parcelas());
        for (int indice = 0; indice < decisao.parcelas(); indice++) {
            String descricaoDaParcela = decisao.descricao() + " (parcela " + (indice + 1) + "/" + decisao.parcelas() + ")";
            parcelas.add(new Parcela(decisao.data().plusMonths(indice), descricaoDaParcela, valoresRateados.get(indice)));
        }
        return parcelas;
    }

    private UsoDeCredito encontraClassificacaoDaPrimeiraParcela(List<PlanilhaDoMes> cenarioSimulado, ComandoDeDecisao decisao) {
        if (decisao.formaPagamento() == FormaPagamento.DEBITO) {
            return null;
        }
        return cenarioSimulado.stream()
                .flatMap(mes -> mes.dias().stream())
                .filter(dia -> dia.data().equals(decisao.data()))
                .flatMap(dia -> dia.lancamentos().stream())
                .filter(item -> item.descricao().startsWith(decisao.descricao()))
                .map(ItemDoDia::usoDeCredito)
                .findFirst()
                .orElse(null);
    }

    private record Parcela(LocalDate vencimento, String descricao, Dinheiro valor) {
    }
}

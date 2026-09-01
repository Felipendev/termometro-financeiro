package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.planilha.domain.ItemDoDia;
import br.com.felipe.termometro.planilha.domain.PlanilhaDoMes;
import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PlanilhaService {
    default PlanilhaDoMes consulta(Competencia competencia) {
        return consultaComItensExtras(competencia, Map.of());
    }

    /**
     * RN-23.1 — mesma leitura de {@link #consulta}, mas com lançamentos hipotéticos somados por
     * cima, sem persistir nada. É assim que o simulador de decisão compara real × simulado sem
     * duplicar a lógica de cascata.
     */
    PlanilhaDoMes consultaComItensExtras(Competencia competencia, Map<LocalDate, List<ItemDoDia>> itensExtras);

    SaldoInicialPlanilha defineSaldoInicial(LocalDate dataReferencia, Dinheiro valor);

    void sobrescreveDiario(LocalDate data, Dinheiro valor);

    void sobrescreveDiarioEmSerie(LocalDate de, LocalDate ate, Dinheiro valor);

    void defineObservacao(LocalDate data, String texto);

    LancamentoPlanejado adicionaLancamento(LancamentoPlanejado item);

    LancamentoPlanejado editaLancamento(LancamentoPlanejado item);

    void removeLancamento(UUID id);
}

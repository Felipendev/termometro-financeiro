package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.UUID;

public interface SimuladorDeDecisaoService {

    ResultadoDaSimulacao simula(ComandoDeDecisao decisao, Competencia de, Competencia ate);

    /** RN-23.2 — só aqui a decisão vira `LancamentoPlanejado` de verdade, um por parcela. */
    List<UUID> confirma(ComandoDeDecisao decisao);
}

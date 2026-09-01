package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.shared.Competencia;
import java.util.List;

/** Resumo da análise disparada depois de uma importação com transações realmente novas. */
public record ResultadoDoProcessamentoImportacao(
        List<Competencia> competenciasProcessadas,
        int classificadas,
        int pendentesDeRevisao,
        int triadas) {

    public ResultadoDoProcessamentoImportacao {
        competenciasProcessadas = List.copyOf(competenciasProcessadas);
    }

    public static ResultadoDoProcessamentoImportacao vazio() {
        return new ResultadoDoProcessamentoImportacao(List.of(), 0, 0, 0);
    }
}

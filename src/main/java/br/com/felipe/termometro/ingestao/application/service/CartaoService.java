package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.shared.Competencia;

/**
 * Porta de leitura da visão "Cartões" — gasto real por cartão de crédito, direto das transações
 * já sincronizadas. Diferente de {@link DiagnosticoService}/{@code TriagemService}, não depende
 * de piso humano, classificação ou triagem terem rodado: é soma bruta, disponível assim que o
 * sync trouxe a transação.
 */
public interface CartaoService {

    ResumoCartoesResponse consultaCartoes(Competencia competencia);
}

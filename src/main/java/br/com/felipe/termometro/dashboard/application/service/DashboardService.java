package br.com.felipe.termometro.dashboard.application.service;

import br.com.felipe.termometro.dashboard.application.api.response.DashboardResponse;
import br.com.felipe.termometro.shared.Competencia;

/**
 * Porta de entrada do dashboard agregado (RN-11, visão de leitura).
 *
 * <p>Diferente das demais portas do sistema, devolve diretamente o tipo de resposta da API em vez
 * de um objeto de domínio: este módulo não introduz nenhuma regra de negócio nova, só recompõe
 * saídas que já são objetos de domínio de outros módulos (ou, no caso de {@link
 * br.com.felipe.termometro.triagem.application.service.TriagemService#resumo}, já é o próprio DTO
 * de resposta — precedente existente, não criado por este módulo). Um record de domínio aqui
 * seria imediatamente desempacotado pelo controller sem agregar nada; por isso {@code dashboard}
 * não tem pacote {@code domain}.
 */
public interface DashboardService {

    DashboardResponse monta(Competencia competencia);
}

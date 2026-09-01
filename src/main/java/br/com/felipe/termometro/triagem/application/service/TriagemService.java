package br.com.felipe.termometro.triagem.application.service;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import java.util.List;
import java.util.UUID;

public interface TriagemService {

    /** Roda o algoritmo do piso (RN-05) sobre todas as transações já classificadas do mês. */
    ResultadoDaTriagemResponse executaTriagem(Competencia competencia);

    /** Totais por categoria e cor, recalculados na leitura. */
    List<ResumoDeCategoriaResponse> resumo(Competencia competencia);

    /** Promoção manual para VERMELHA. Só é permitida a partir de uma transação hoje AMARELA. */
    void promoveParaVermelha(UUID transacaoId);
}

package br.com.felipe.termometro.classificacao.application.service;

import br.com.felipe.termometro.classificacao.application.api.request.ClassificarTransacaoRequest;
import br.com.felipe.termometro.classificacao.application.api.response.ContextoDeRevisaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaCorrecaoResponse;
import java.util.List;
import java.util.UUID;
import br.com.felipe.termometro.shared.Competencia;

public interface ClassificacaoService {

    /** Classifica o que ainda não foi classificado no mês. */
    ResultadoDaClassificacaoResponse classifica(Competencia competencia);

    /**
     * Reclassifica <b>tudo</b> do mês. Usada depois que o usuário cria uma regra: a decisão dele
     * precisa valer para o passado, não só para o futuro (RN-17).
     */
    ResultadoDaClassificacaoResponse reclassifica(Competencia competencia);

    /** Fila da RN-12: o que o sistema não conseguiu decidir, com contexto para o usuário decidir. */
    List<ContextoDeRevisaoResponse> filaDeRevisao(Competencia competencia, int limite);

    /** O usuário corrigindo — opcionalmente ensinando o sistema para o grupo inteiro. */
    ResultadoDaCorrecaoResponse corrige(UUID transacaoId, ClassificarTransacaoRequest request);
}

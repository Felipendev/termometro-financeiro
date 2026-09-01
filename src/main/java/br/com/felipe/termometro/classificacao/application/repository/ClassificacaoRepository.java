package br.com.felipe.termometro.classificacao.application.repository;

import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.Classificacao;
import br.com.felipe.termometro.classificacao.domain.ContextoDeRevisao;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ClassificacaoRepository {

    /** Transações do mês que ainda não foram classificadas, ou que precisam de reclassificação. */
    Map<UUID, TransacaoBruta> buscaParaClassificar(Competencia competencia, boolean apenasNaoClassificadas);

    /** Grava a classificação de várias transações de uma vez. */
    int aplica(Map<UUID, Classificacao> classificacoes);

    /** Fila da RN-12: o que o sistema não decidiu sozinho, com o contexto para decidir. */
    List<ContextoDeRevisao> buscaFilaDeRevisao(Competencia competencia, int limite);

    /** Uma transação específica, com o contexto — para a tela de correção. */
    java.util.Optional<ContextoDeRevisao> buscaContexto(UUID id);

    /**
     * Aplica a categoria a todas as transações do mesmo grupo de similaridade (RN-12).
     * É o que faz classificar uma resolver quarenta.
     *
     * @return quantas transações foram reclassificadas
     */
    int aplicaAoGrupo(String grupoDeSimilaridade, Categoria categoria, boolean contaNoDiaADia);
}

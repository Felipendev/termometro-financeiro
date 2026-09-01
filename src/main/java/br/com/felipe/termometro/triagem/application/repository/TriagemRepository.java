package br.com.felipe.termometro.triagem.application.repository;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.triagem.domain.Etiqueta;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída da triagem. Lê e escreve na mesma tabela {@code transacao} da ingestão. */
public interface TriagemRepository {

    /** Transações já classificadas (RN-12) do mês, com a etiqueta atual quando houver. */
    List<TransacaoClassificada> buscaClassificadasDoMes(Competencia competencia);

    /** Grava as etiquetas decididas pelo motor. */
    int aplicaEtiquetas(Map<UUID, Etiqueta> etiquetas);

    /** A etiqueta atual de uma transação específica — para validar a promoção manual. */
    Optional<Etiqueta> buscaEtiquetaAtual(UUID transacaoId);

    /** Promove manualmente para VERMELHA. Não valida a regra — quem valida é o serviço. */
    void promoveParaVermelha(UUID transacaoId);
}

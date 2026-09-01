package br.com.felipe.termometro.triagem.infra;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.repository.TriagemRepository;
import br.com.felipe.termometro.triagem.domain.Etiqueta;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TriagemInfraRepository implements TriagemRepository {

    private final TriagemSpringDataJpaRepository transacaoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<TransacaoClassificada> buscaClassificadasDoMes(Competencia competencia) {
        log.info("[inicia] TriagemInfraRepository - buscaClassificadasDoMes [{}]", competencia);
        List<TransacaoClassificada> transacoes = transacaoRepository
                .buscaClassificadasDoMes(competencia.primeiroDia(), competencia.ultimoDia())
                .stream()
                .map(TriagemInfraRepository::paraDominio)
                .toList();
        log.info("[finaliza] TriagemInfraRepository - buscaClassificadasDoMes [{}]", transacoes.size());
        return transacoes;
    }

    /**
     * Mesma técnica de {@code ClassificacaoInfraRepository.aplica}: {@code find} + mutação dentro
     * da transação, não {@code UPDATE} em massa — o bulk update do JPA ignora o contexto de
     * persistência e deixaria entidades já carregadas na mesma transação com etiqueta velha.
     */
    @Override
    @Transactional
    public int aplicaEtiquetas(Map<UUID, Etiqueta> etiquetas) {
        log.info("[inicia] TriagemInfraRepository - aplicaEtiquetas [{}]", etiquetas.size());
        int aplicadas = 0;
        for (Map.Entry<UUID, Etiqueta> entrada : etiquetas.entrySet()) {
            TransacaoJpaEntity entidade = entityManager.find(TransacaoJpaEntity.class, entrada.getKey());
            if (entidade == null) {
                log.warn("[TriagemInfraRepository] transação {} sumiu entre a leitura e a gravação, ignorada",
                        entrada.getKey());
                continue;
            }
            entidade.aplicaEtiqueta(entrada.getValue().name());
            aplicadas++;
        }
        log.info("[finaliza] TriagemInfraRepository - aplicaEtiquetas [{}]", aplicadas);
        return aplicadas;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Etiqueta> buscaEtiquetaAtual(UUID transacaoId) {
        return transacaoRepository.findById(transacaoId)
                .map(TransacaoJpaEntity::getEtiqueta)
                .map(Etiqueta::valueOf);
    }

    @Override
    @Transactional
    public void promoveParaVermelha(UUID transacaoId) {
        TransacaoJpaEntity entidade = entityManager.find(TransacaoJpaEntity.class, transacaoId);
        if (entidade == null) {
            throw APIException.build(HttpStatus.NOT_FOUND, "Transação não encontrada: " + transacaoId);
        }
        entidade.aplicaEtiqueta(Etiqueta.VERMELHA.name());
    }

    private static TransacaoClassificada paraDominio(TransacaoJpaEntity entidade) {
        Etiqueta etiquetaAtual = entidade.getEtiqueta() == null ? null : Etiqueta.valueOf(entidade.getEtiqueta());
        return new TransacaoClassificada(
                entidade.getId(),
                entidade.getData(),
                Dinheiro.de(entidade.getValor().abs()),
                entidade.getCategoria(),
                Natureza.valueOf(entidade.getNatureza()),
                etiquetaAtual);
    }
}

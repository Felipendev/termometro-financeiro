package br.com.felipe.termometro.classificacao.infra;

import br.com.felipe.termometro.classificacao.application.repository.ClassificacaoRepository;
import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.Classificacao;
import br.com.felipe.termometro.classificacao.domain.ContextoDeRevisao;
import br.com.felipe.termometro.classificacao.domain.PeriodoDoDia;
import br.com.felipe.termometro.classificacao.domain.SugestaoDeCategoria;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ClassificacaoInfraRepository implements ClassificacaoRepository {

    private final ClassificacaoSpringDataJpaRepository transacaoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, TransacaoBruta> buscaParaClassificar(Competencia competencia,
                                                          boolean apenasNaoClassificadas) {
        log.info("[inicia] ClassificacaoInfraRepository - buscaParaClassificar [{}]", competencia);
        Map<UUID, TransacaoBruta> transacoes = new LinkedHashMap<>();
        transacaoRepository
                .buscaParaClassificar(competencia.primeiroDia(), competencia.ultimoDia(),
                        !apenasNaoClassificadas)
                .forEach(entidade -> transacoes.put(entidade.getId(), entidade.paraDominio()));
        log.info("[finaliza] ClassificacaoInfraRepository - buscaParaClassificar [{}]", transacoes.size());
        return transacoes;
    }

    /**
     * Grava as classificações.
     *
     * <p>Usa {@code find} + mutação dentro da transação em vez de {@code UPDATE} em massa: o bulk
     * update do JPA ignora o contexto de persistência e deixaria entidades já carregadas com valor
     * velho na mesma transação — o tipo de inconsistência que só aparece quando a sincronização e a
     * classificação rodam na mesma requisição, que é exatamente o caso aqui.
     */
    @Override
    @Transactional
    public int aplica(Map<UUID, Classificacao> classificacoes) {
        log.info("[inicia] ClassificacaoInfraRepository - aplica [{}]", classificacoes.size());
        int aplicadas = 0;
        for (Map.Entry<UUID, Classificacao> entrada : classificacoes.entrySet()) {
            TransacaoJpaEntity entidade = entityManager.find(TransacaoJpaEntity.class, entrada.getKey());
            if (entidade == null) {
                log.warn("[ClassificacaoInfraRepository] transação {} sumiu entre a leitura e a "
                        + "gravação, ignorada", entrada.getKey());
                continue;
            }
            aplicaNaEntidade(entidade, entrada.getValue());
            aplicadas++;
        }
        log.info("[finaliza] ClassificacaoInfraRepository - aplica [{}]", aplicadas);
        return aplicadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextoDeRevisao> buscaFilaDeRevisao(Competencia competencia, int limite) {
        log.info("[inicia] ClassificacaoInfraRepository - buscaFilaDeRevisao [{}]", competencia);
        // Ordenado pelo valor: R$ 400 sem categoria distorce o mês muito mais que R$ 4, e o tempo
        // de revisão do usuário é o recurso escasso aqui.
        List<ContextoDeRevisao> fila = transacaoRepository
                .buscaFilaDeRevisao(competencia.primeiroDia(), competencia.ultimoDia(),
                        PageRequest.of(0, Math.max(1, limite)))
                .stream()
                .map(this::montaContexto)
                .toList();
        log.info("[finaliza] ClassificacaoInfraRepository - buscaFilaDeRevisao [{}]", fila.size());
        return fila;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContextoDeRevisao> buscaContexto(UUID id) {
        return transacaoRepository.findByIdAndOrigemNot(id, Origem.MANUAL).map(this::montaContexto);
    }

    @Override
    @Transactional
    public int aplicaAoGrupo(String grupoDeSimilaridade, Categoria categoria, boolean contaNoDiaADia) {
        log.info("[inicia] ClassificacaoInfraRepository - aplicaAoGrupo [{}]", grupoDeSimilaridade);
        List<TransacaoJpaEntity> doGrupo =
                transacaoRepository.findByDescricaoNormalizada(grupoDeSimilaridade);
        for (TransacaoJpaEntity entidade : doGrupo) {
            // Parcela e pagamento continuam fora da verba mesmo com categoria variável — a regra
            // da RN-19 não é sobrescrita por uma correção de categoria.
            boolean entra = contaNoDiaADia
                    && entidade.paraDominio().ehDespesa()
                    && entidade.paraDominio().compoeTotalDaFatura()
                    && entidade.paraDominio().parcelaOpcional().isEmpty();
            entidade.aplicaClassificacao(categoria.nome(), categoria.grupo().name(),
                    categoria.natureza().name(), BigDecimal.ONE,
                    br.com.felipe.termometro.classificacao.domain.OrigemDaRegra.USUARIO.name(),
                    entra, false);
        }
        log.info("[finaliza] ClassificacaoInfraRepository - aplicaAoGrupo [{}]", doGrupo.size());
        return doGrupo.size();
    }

    private void aplicaNaEntidade(TransacaoJpaEntity entidade, Classificacao classificacao) {
        entidade.aplicaClassificacao(
                classificacao.categoria().nome(),
                classificacao.categoria().grupo().name(),
                classificacao.categoria().natureza().name(),
                classificacao.confianca(),
                classificacao.origem() == null ? null : classificacao.origem().name(),
                classificacao.contaNoDiaADia(),
                classificacao.precisaRevisao());
    }

    private ContextoDeRevisao montaContexto(TransacaoJpaEntity entidade) {
        String grupo = entidade.getDescricaoNormalizada();
        Object[] estatisticas = transacaoRepository.estatisticasDoGrupo(grupo).stream()
                .findFirst().orElse(null);
        long total = estatisticas == null ? 1L : ((Number) estatisticas[0]).longValue();
        BigDecimal ticket = estatisticas == null || estatisticas[1] == null
                ? entidade.getValor().abs()
                : new BigDecimal(estatisticas[1].toString());

        List<SugestaoDeCategoria> sugestoes = new ArrayList<>();
        if (entidade.getCategoriaBanco() != null && !entidade.getCategoriaBanco().isBlank()) {
            sugestoes.add(new SugestaoDeCategoria(Categoria.NAO_IDENTIFICADA,
                    new BigDecimal("0.68"),
                    "o banco classificou como '" + entidade.getCategoriaBanco() + "'"));
        }

        return new ContextoDeRevisao(
                entidade.getId(),
                entidade.getDescricao(),
                entidade.getDescricaoOriginal(),
                grupo,
                Dinheiro.de(entidade.getValor()),
                entidade.getData(),
                entidade.getData().getDayOfWeek(),
                PeriodoDoDia.de(entidade.getDataHora(), entidade.isHoraConfiavel()).orElse(null),
                entidade.isHoraConfiavel(),
                // O próprio lançamento não conta como "outro igual".
                (int) Math.max(0, total - 1),
                Dinheiro.de(ticket),
                sugestoes);
    }
}

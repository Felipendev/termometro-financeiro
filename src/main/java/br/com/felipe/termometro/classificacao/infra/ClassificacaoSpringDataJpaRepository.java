package br.com.felipe.termometro.classificacao.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import br.com.felipe.termometro.ingestao.domain.Origem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Leitura das transações a classificar. Aponta para a entidade da ingestão porque é lá que a
 * transação mora — o acoplamento entre os módulos fica no banco, não nos tipos de domínio.
 */
interface ClassificacaoSpringDataJpaRepository extends Repository<TransacaoJpaEntity, UUID> {

    @Query("""
            select t from TransacaoJpaEntity t
             where t.data between :primeiroDia and :ultimoDia
               and t.ignorada = false
               and (:todas = true or t.classificadoEm is null)
             order by t.data
            """)
    List<TransacaoJpaEntity> buscaParaClassificar(@Param("primeiroDia") LocalDate primeiroDia,
                                                  @Param("ultimoDia") LocalDate ultimoDia,
                                                  @Param("todas") boolean todas);

    @Query("""
            select t from TransacaoJpaEntity t
             where t.data between :primeiroDia and :ultimoDia
               and t.ignorada = false
               and t.precisaRevisao = true
               and t.valor < 0
             order by t.valor asc
            """)
    List<TransacaoJpaEntity> buscaFilaDeRevisao(@Param("primeiroDia") LocalDate primeiroDia,
                                                @Param("ultimoDia") LocalDate ultimoDia,
                                                org.springframework.data.domain.Pageable paginacao);

    java.util.Optional<TransacaoJpaEntity> findById(UUID id);

    java.util.Optional<TransacaoJpaEntity> findByIdAndOrigemNot(UUID id, Origem origem);

    /**
     * Estatísticas do grupo de similaridade: quantas são e qual o ticket médio. É o número que diz
     * ao usuário se vale a pena aplicar ao grupo.
     */
    @Query("""
            select count(t), coalesce(avg(abs(t.valor)), 0)
              from TransacaoJpaEntity t
             where t.descricaoNormalizada = :grupo
               and t.ignorada = false
            """)
    List<Object[]> estatisticasDoGrupo(@Param("grupo") String grupo);

    @Query("select t from TransacaoJpaEntity t "
            + "where t.descricaoNormalizada = :grupo and t.ignorada = false")
    List<TransacaoJpaEntity> findByDescricaoNormalizada(@Param("grupo") String descricaoNormalizada);
}

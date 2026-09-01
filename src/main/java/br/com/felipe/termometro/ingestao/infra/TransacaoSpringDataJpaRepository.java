package br.com.felipe.termometro.ingestao.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TransacaoSpringDataJpaRepository extends JpaRepository<TransacaoJpaEntity, UUID> {

    @Query("select t.hashDedupe from TransacaoJpaEntity t "
            + "where t.identificadorConta = :conta and t.hashDedupe in :hashes and t.ignorada = false")
    Set<String> buscaHashesExistentes(@Param("conta") String conta,
                                      @Param("hashes") Set<String> hashes);

    @Query("select t from TransacaoJpaEntity t "
            + "where t.data between :inicio and :fim and t.ignorada = false "
            + "order by t.data asc")
    List<TransacaoJpaEntity> findByDataBetweenOrderByDataAsc(
            @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    List<TransacaoJpaEntity> findByLancamentoPlanejadoId(UUID lancamentoPlanejadoId);

    @Query("select t.identificadorConta as identificadorConta, sum(t.valor) as total "
            + "from TransacaoJpaEntity t "
            + "where t.secao = br.com.felipe.termometro.ingestao.domain.SecaoFatura.CARTAO "
            + "and t.ignorada = false and t.data between :inicio and :fim "
            + "group by t.identificadorConta")
    List<GastoPorContaProjection> somaPorContaCartao(
            @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}

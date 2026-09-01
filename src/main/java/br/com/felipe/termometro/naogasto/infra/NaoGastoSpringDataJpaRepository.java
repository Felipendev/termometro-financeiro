package br.com.felipe.termometro.naogasto.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Leitura das transações a conciliar. Aponta para a entidade da ingestão pelo mesmo motivo
 * documentado em {@code ClassificacaoSpringDataJpaRepository} e {@code TriagemSpringDataJpaRepository}:
 * o acoplamento entre módulos fica no banco, não nos tipos de domínio.
 */
interface NaoGastoSpringDataJpaRepository extends Repository<TransacaoJpaEntity, UUID> {

    @Query("select t from TransacaoJpaEntity t "
            + "where t.data between :inicio and :fim and t.ignorada = false "
            + "order by t.data")
    List<TransacaoJpaEntity> buscaNaoIgnoradas(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}

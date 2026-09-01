package br.com.felipe.termometro.triagem.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Leitura e escrita das transações a triar. Aponta para a entidade da ingestão pelo mesmo motivo
 * documentado em {@code ClassificacaoSpringDataJpaRepository}: o acoplamento entre módulos fica no
 * banco, não nos tipos de domínio.
 */
interface TriagemSpringDataJpaRepository extends Repository<TransacaoJpaEntity, UUID> {

    @Query("""
            select t from TransacaoJpaEntity t
             where t.data between :primeiroDia and :ultimoDia
               and t.categoria is not null
               and t.ignorada = false
             order by t.data
            """)
    List<TransacaoJpaEntity> buscaClassificadasDoMes(@Param("primeiroDia") LocalDate primeiroDia,
                                                     @Param("ultimoDia") LocalDate ultimoDia);

    Optional<TransacaoJpaEntity> findById(UUID id);
}

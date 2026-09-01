package br.com.felipe.termometro.compromissofuturo.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import java.util.UUID;

/**
 * Leitura das transações parceladas. Aponta para a entidade da ingestão pelo mesmo motivo
 * documentado em {@code NaoGastoSpringDataJpaRepository}: o acoplamento entre módulos fica no
 * banco, não nos tipos de domínio.
 */
interface LancamentoParceladoSpringDataJpaRepository extends Repository<TransacaoJpaEntity, UUID> {

    @Query("select t from TransacaoJpaEntity t "
            + "where t.parcelaNumero is not null and t.parcelaTotal is not null and t.ignorada = false "
            + "order by t.data")
    List<TransacaoJpaEntity> buscaParceladas();
}

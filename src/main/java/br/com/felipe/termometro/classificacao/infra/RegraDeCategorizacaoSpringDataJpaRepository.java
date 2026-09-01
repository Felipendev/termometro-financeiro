package br.com.felipe.termometro.classificacao.infra;

import br.com.felipe.termometro.classificacao.domain.OrigemDaRegra;
import br.com.felipe.termometro.classificacao.domain.TipoDeRegra;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegraDeCategorizacaoSpringDataJpaRepository
        extends JpaRepository<RegraDeCategorizacaoJpaEntity, UUID> {

    List<RegraDeCategorizacaoJpaEntity> findAllByOrderByPrioridadeAsc();

    Optional<RegraDeCategorizacaoJpaEntity> findByTipoAndPadraoAndOrigem(
            TipoDeRegra tipo, String padrao, OrigemDaRegra origem);
}

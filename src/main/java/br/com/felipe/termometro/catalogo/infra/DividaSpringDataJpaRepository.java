package br.com.felipe.termometro.catalogo.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DividaSpringDataJpaRepository extends JpaRepository<DividaJpaEntity, UUID> {

    /** Ainda ativa se a última parcela cai na competência consultada ou depois dela. */
    List<DividaJpaEntity> findByCompetenciaUltimaParcelaGreaterThanEqualOrderByNome(LocalDate competencia);
}

package br.com.felipe.termometro.catalogo.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DividaRotativaSpringDataJpaRepository extends JpaRepository<DividaRotativaJpaEntity, UUID> {

    List<DividaRotativaJpaEntity> findBySaldoDevedorGreaterThanOrderByNome(BigDecimal saldoDevedor);
}

package br.com.felipe.termometro.catalogo.infra;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CustoFixoItemSpringDataJpaRepository extends JpaRepository<CustoFixoItemJpaEntity, UUID> {

    List<CustoFixoItemJpaEntity> findByAtivoTrueOrderByNome();
}

package br.com.felipe.termometro.cartao.infra;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartaoSpringDataJpaRepository extends JpaRepository<CartaoJpaEntity, UUID> {

    List<CartaoJpaEntity> findByAtivoTrueOrderByNome();
}

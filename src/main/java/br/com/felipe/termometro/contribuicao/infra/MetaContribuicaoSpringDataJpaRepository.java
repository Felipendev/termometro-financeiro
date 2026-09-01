package br.com.felipe.termometro.contribuicao.infra;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaContribuicaoSpringDataJpaRepository extends JpaRepository<MetaContribuicaoJpaEntity, String> {
}

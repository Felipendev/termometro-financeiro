package br.com.felipe.termometro.ingestao.infra;

import br.com.felipe.termometro.ingestao.domain.TipoDeConta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ContaSpringDataJpaRepository extends JpaRepository<ContaJpaEntity, UUID> {

    Optional<ContaJpaEntity> findByIdentificador(String identificador);

    List<ContaJpaEntity> findByTipoOrderByNome(TipoDeConta tipo);
}

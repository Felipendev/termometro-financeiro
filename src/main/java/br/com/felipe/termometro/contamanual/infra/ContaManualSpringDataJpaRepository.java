package br.com.felipe.termometro.contamanual.infra;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
interface ContaManualSpringDataJpaRepository extends JpaRepository<ContaManualJpaEntity, UUID> { List<ContaManualJpaEntity> findByAtivaTrueOrderByNome(); }

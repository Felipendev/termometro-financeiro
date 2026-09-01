package br.com.felipe.termometro.lancamentoplanejado.infra;

import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface LancamentoPlanejadoSpringDataJpaRepository extends JpaRepository<LancamentoPlanejadoJpaEntity, UUID> {
    List<LancamentoPlanejadoJpaEntity> findByStatusOrderByVencimento(StatusLancamentoPlanejado status);
    List<LancamentoPlanejadoJpaEntity> findByVencimentoBetweenOrderByVencimentoDesc(
            LocalDate inicio, LocalDate fim);
    Optional<LancamentoPlanejadoJpaEntity> findFirstByOrderByVencimentoAsc();
}

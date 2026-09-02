package br.com.felipe.termometro.lancamentoplanejado.infra;

import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LancamentoPlanejadoSpringDataJpaRepository extends JpaRepository<LancamentoPlanejadoJpaEntity, UUID> {
    List<LancamentoPlanejadoJpaEntity> findByStatusOrderByVencimento(StatusLancamentoPlanejado status);
    List<LancamentoPlanejadoJpaEntity> findByVencimentoBetweenOrderByVencimentoDesc(
            LocalDate inicio, LocalDate fim);
    Optional<LancamentoPlanejadoJpaEntity> findFirstByOrderByVencimentoAsc();
    List<LancamentoPlanejadoJpaEntity> findBySerieIdOrderByVencimento(UUID serieId);

    @Query("select distinct t.serieId from LancamentoPlanejadoJpaEntity t "
            + "where t.serieId is not null and t.status = 'PENDENTE'")
    List<UUID> buscaSeriesComPendencia();

    @Query("select t from LancamentoPlanejadoJpaEntity t "
            + "where t.diaRecorrencia is not null and t.serieId is null "
            + "and t.status <> 'CANCELADO' order by t.vencimento")
    List<LancamentoPlanejadoJpaEntity> buscaOrfaosDeRecorrencia();
}

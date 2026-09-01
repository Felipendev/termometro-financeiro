package br.com.felipe.termometro.compromissofuturo.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CompromissoFuturoSpringDataJpaRepository extends JpaRepository<CompromissoFuturoJpaEntity, UUID> {

    List<CompromissoFuturoJpaEntity> findByCompetenciaBetween(LocalDate inicio, LocalDate fim);

    @Modifying
    @Query("delete from CompromissoFuturoJpaEntity c "
            + "where c.identificadorConta = :conta and c.descricaoNormalizada = :descricaoNormalizada "
            + "and c.parcelaTotal = :parcelaTotal")
    void apagaSerie(@Param("conta") String identificadorConta,
            @Param("descricaoNormalizada") String descricaoNormalizada, @Param("parcelaTotal") int parcelaTotal);
}

package br.com.felipe.termometro.catalogo.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PisoHumanoSpringDataJpaRepository extends JpaRepository<PisoHumanoJpaEntity, UUID> {

    List<PisoHumanoJpaEntity> findAllByOrderByCategoria();

    Optional<PisoHumanoJpaEntity> findByCategoria(String categoria);

    /** Derivada por nome de método — ao contrário de {@code deleteById}, não lança se não achar nada. */
    void deleteByCategoria(String categoria);
}

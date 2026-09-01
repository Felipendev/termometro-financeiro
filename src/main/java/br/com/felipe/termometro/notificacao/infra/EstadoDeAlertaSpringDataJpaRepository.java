package br.com.felipe.termometro.notificacao.infra;

import org.springframework.data.jpa.repository.JpaRepository;

interface EstadoDeAlertaSpringDataJpaRepository extends JpaRepository<EstadoDeAlertaJpaEntity, String> {
}

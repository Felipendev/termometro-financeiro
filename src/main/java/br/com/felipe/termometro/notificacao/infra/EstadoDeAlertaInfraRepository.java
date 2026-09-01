package br.com.felipe.termometro.notificacao.infra;

import br.com.felipe.termometro.notificacao.application.repository.EstadoDeAlertaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code save} faz upsert por construção: a chave é sempre atribuída manualmente (nunca gerada),
 * então o Spring Data reconhece a entidade como "existente" e o Hibernate faz {@code merge} —
 * insere se a chave for nova, atualiza o valor se já existir.
 */
@Repository
@RequiredArgsConstructor
public class EstadoDeAlertaInfraRepository implements EstadoDeAlertaRepository {

    private final EstadoDeAlertaSpringDataJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> busca(String chave) {
        return repository.findById(chave).map(EstadoDeAlertaJpaEntity::getValor);
    }

    @Override
    @Transactional
    public void salva(String chave, String valor) {
        repository.save(new EstadoDeAlertaJpaEntity(chave, valor));
    }
}

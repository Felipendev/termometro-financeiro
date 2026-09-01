package br.com.felipe.termometro.cartao.infra;

import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CartaoInfraRepository implements CartaoRepository {

    private final CartaoSpringDataJpaRepository cartaoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Cartao> buscaAtivos() {
        return cartaoRepository.findByAtivoTrueOrderByNome().stream()
                .map(CartaoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public Cartao salva(Cartao cartao) {
        return cartaoRepository.save(new CartaoJpaEntity(cartao)).paraDominio();
    }

    @Override
    @Transactional
    public void remove(UUID id) {
        cartaoRepository.findById(id).ifPresent(entidade -> {
            entidade.desativa();
            cartaoRepository.save(entidade);
        });
    }
}

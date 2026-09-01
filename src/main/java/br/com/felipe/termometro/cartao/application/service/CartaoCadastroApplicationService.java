package br.com.felipe.termometro.cartao.application.service;

import br.com.felipe.termometro.cartao.application.api.request.CartaoRequest;
import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartaoCadastroApplicationService implements CartaoCadastroService {

    private final CartaoRepository cartaoRepository;

    @Override
    public List<Cartao> listaAtivos() {
        return cartaoRepository.buscaAtivos();
    }

    @Override
    public Cartao salva(UUID id, CartaoRequest request) {
        log.info("[inicia] CartaoCadastroApplicationService - salva [{}]", id);
        Cartao cartao = cartaoRepository.salva(request.paraDominio(id));
        log.info("[finaliza] CartaoCadastroApplicationService - salva [{}]", id);
        return cartao;
    }

    @Override
    public void remove(UUID id) {
        log.info("[inicia] CartaoCadastroApplicationService - remove [{}]", id);
        cartaoRepository.remove(id);
        log.info("[finaliza] CartaoCadastroApplicationService - remove [{}]", id);
    }
}

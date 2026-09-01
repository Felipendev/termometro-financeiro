package br.com.felipe.termometro.cartao.application.api;

import br.com.felipe.termometro.cartao.application.api.request.CartaoRequest;
import br.com.felipe.termometro.cartao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.cartao.application.service.CartaoCadastroService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CartaoCadastroRestController implements CartaoCadastroAPI {

    private final CartaoCadastroService cartaoCadastroService;

    @Override
    public List<CartaoResponse> getCartoesManuais() {
        return cartaoCadastroService.listaAtivos().stream().map(CartaoResponse::new).toList();
    }

    @Override
    public CartaoResponse putCartaoManual(UUID id, CartaoRequest request) {
        return new CartaoResponse(cartaoCadastroService.salva(id, request));
    }

    @Override
    public void deleteCartaoManual(UUID id) {
        cartaoCadastroService.remove(id);
    }
}

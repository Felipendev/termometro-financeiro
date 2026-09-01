package br.com.felipe.termometro.cartao.application.api;

import br.com.felipe.termometro.cartao.application.api.request.CartaoRequest;
import br.com.felipe.termometro.cartao.application.api.response.CartaoResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro manual de cartão — stopgap até o spike do endpoint {@code bills} da Pluggy (ROADMAP)
 * permitir buscar a fatura de verdade. Complementa {@code GET /v1/cartoes} (leitura automática,
 * gasto real por transação — módulo {@code ingestao}, inalterado): aqui é Felipe quem declara o
 * cartão e o valor da fatura, um {@code PUT} por vez, igual ao restante do catálogo (RN-17).
 *
 * <p>{@code DELETE} é soft delete (ver {@code CartaoRepository}): ao contrário de {@code
 * CustoFixoItem} (sem verbo de remoção, só {@code ativo: false} via {@code PUT}), aqui existe um
 * verbo dedicado — o cadastro continua na base, só sai da listagem.
 */
@RestController
@RequestMapping("/v1/cartoes/manuais")
public interface CartaoCadastroAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<CartaoResponse> getCartoesManuais();

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    CartaoResponse putCartaoManual(@PathVariable UUID id, @RequestBody @Valid CartaoRequest request);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCartaoManual(@PathVariable UUID id);
}

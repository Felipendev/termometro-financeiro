package br.com.felipe.termometro.ingestao.application.api;

import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Visão "Cartões" (extensão da fatia 13) — gasto real por cartão de crédito na competência,
 * direto das transações sincronizadas. Só leitura: o cadastro de conta é automático via sync
 * (ver {@code ContaRepository}), nunca manual.
 */
@RestController
@RequestMapping("/v1/cartoes")
public interface CartaoAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResumoCartoesResponse getCartoes(@RequestParam String competencia);
}

package br.com.felipe.termometro.cartao.application.service;

import br.com.felipe.termometro.cartao.application.api.request.CartaoRequest;
import br.com.felipe.termometro.cartao.domain.Cartao;
import java.util.List;
import java.util.UUID;

/**
 * Porta de entrada do cadastro manual de cartão — mesmo papel que {@code CatalogoService} exerce
 * pro catálogo: só a API pública passa por esta camada, o {@code dashboard} lê direto de {@code
 * CartaoRepository} (mesmo padrão de composição já usado com {@code CatalogoRepository}).
 */
public interface CartaoCadastroService {

    List<Cartao> listaAtivos();

    /** Upsert — {@code id} vem do path (gerado no cliente para um cartão novo). */
    Cartao salva(UUID id, CartaoRequest request);

    /** Soft delete idempotente — ver {@code CartaoRepository#remove}. */
    void remove(UUID id);
}

package br.com.felipe.termometro.cartao.application.repository;

import br.com.felipe.termometro.cartao.domain.Cartao;
import java.util.List;
import java.util.UUID;

/** Porta de saída do cadastro manual de cartão. */
public interface CartaoRepository {

    /** Só cartões ativos, ordenados por nome. */
    List<Cartao> buscaAtivos();

    /** Upsert por {@link Cartao#id()}. */
    Cartao salva(Cartao cartao);

    /**
     * Soft delete — marca {@code ativo = false} em vez de apagar a linha (preserva o cadastro
     * pra correlação futura com {@code bills} da Pluggy). Idempotente: id inexistente ou já
     * inativo não é erro.
     */
    void remove(UUID id);
}

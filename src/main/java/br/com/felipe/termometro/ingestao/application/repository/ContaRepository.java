package br.com.felipe.termometro.ingestao.application.repository;

import br.com.felipe.termometro.ingestao.domain.ContaBancaria;
import java.util.List;

/**
 * Porta de saída para a persistência de conta (nome/tipo/limite/saldo, ver {@link ContaBancaria}).
 * Alimenta a visão "Cartões" — o cadastro em si é 100% automático via sync, nunca manual.
 */
public interface ContaRepository {

    /** Upsert por {@link ContaBancaria#identificador()} — chamado uma vez por conta a cada sync. */
    void salva(ContaBancaria conta);

    /** Só contas de cartão de crédito, ordenadas por nome — é a lista que a tela "Cartões" usa. */
    List<ContaBancaria> buscaCartoes();
}

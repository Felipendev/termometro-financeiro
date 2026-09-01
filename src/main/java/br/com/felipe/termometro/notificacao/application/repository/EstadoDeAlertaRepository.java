package br.com.felipe.termometro.notificacao.application.repository;

import java.util.Optional;

/**
 * Porta de saída: memória de "já avisei isso" para os alertas que não podem se repetir todo dia
 * (RN-22). Chave-valor genérico — cada gatilho decide sozinho o formato da sua própria chave.
 */
public interface EstadoDeAlertaRepository {

    Optional<String> busca(String chave);

    void salva(String chave, String valor);
}

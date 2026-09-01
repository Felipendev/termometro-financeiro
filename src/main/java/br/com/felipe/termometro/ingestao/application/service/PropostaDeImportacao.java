package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * O que a detecção automática (RN-27.1) encontrou — ou não encontrou. {@code formatoDetectado} e
 * {@code leitura} nulos juntos significam "nenhum leitor reconheceu este arquivo", e
 * {@code formatosDisponiveis} vira o fallback de escolha manual.
 */
public record PropostaDeImportacao(
        @Nullable String formatoDetectado,
        @Nullable ResultadoDaLeitura leitura,
        List<String> formatosDisponiveis) {

    public PropostaDeImportacao {
        formatosDisponiveis = List.copyOf(formatosDisponiveis);
    }

    public boolean reconhecido() {
        return formatoDetectado != null;
    }
}

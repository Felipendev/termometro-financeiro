package br.com.felipe.termometro.classificacao.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Palpite oferecido ao usuário na fila de revisão (RN-12), com o porquê junto.
 *
 * <p>O motivo não é enfeite: sugestão sem justificativa vira clique automático, e clique
 * automático ensina o sistema errado — a regra aprendida nasceria de uma decisão que o usuário
 * não chegou a tomar.
 */
public record SugestaoDeCategoria(Categoria categoria, BigDecimal confianca, String motivo)
        implements Comparable<SugestaoDeCategoria> {

    public SugestaoDeCategoria {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(confianca, "confiança não pode ser nula");
        Objects.requireNonNull(motivo, "motivo não pode ser nulo");
    }

    @Override
    public int compareTo(SugestaoDeCategoria outra) {
        return outra.confianca.compareTo(confianca);
    }
}

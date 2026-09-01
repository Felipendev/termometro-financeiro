package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import java.util.Optional;

/**
 * RN-29 — deriva o papel na vida a partir do que a triagem (RN-05) e a natureza já sabem, sem
 * pedir nenhuma categorização nova ao Felipe.
 *
 * <p>{@code QUALIDADE_DE_VIDA} (subconjunto de {@code IMPORTANTE_AJUSTAVEL} para categorias de
 * lazer/restaurante) ficou fora desta primeira versão — exigiria o campo {@code grupo} da
 * categoria, que não chega até {@link TransacaoClassificada}. Fica como refinamento natural
 * quando esse dado estiver disponível aqui.
 */
public final class DerivadorDePapelNaVida {

    private DerivadorDePapelNaVida() {
    }

    /** Vazio para {@link Etiqueta#NAO_TRIADA} — não há papel a atribuir antes de classificar. */
    public static Optional<PapelNaVida> deriva(Etiqueta etiqueta, Natureza natureza) {
        return switch (etiqueta) {
            case VERDE -> Optional.of(PapelNaVida.DIVIDA_COMPROMISSO);
            case VERMELHA -> Optional.of(PapelNaVida.EVITAVEL);
            case AMARELA -> Optional.of(PapelNaVida.REDUTIVEL);
            case AZUL -> Optional.of(derivaDoAzul(natureza));
            case NAO_TRIADA -> Optional.empty();
        };
    }

    private static PapelNaVida derivaDoAzul(Natureza natureza) {
        return natureza == Natureza.VARIAVEL ? PapelNaVida.IMPORTANTE_AJUSTAVEL : PapelNaVida.ESSENCIAL;
    }
}

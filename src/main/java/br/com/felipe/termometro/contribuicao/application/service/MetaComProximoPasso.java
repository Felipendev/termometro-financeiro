package br.com.felipe.termometro.contribuicao.application.service;

import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.ProximoPassoContribuicao;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** {@code proximoPasso} nulo significa "sem espaço ainda" — nunca um passo de valor zero. */
public record MetaComProximoPasso(
        MetaContribuicao meta,
        @Nullable Dinheiro valorMensalAtual,
        @Nullable ProximoPassoContribuicao proximoPasso,
        @Nullable String informacaoNecessaria) {

    public MetaComProximoPasso {
        Objects.requireNonNull(meta, "meta não pode ser nula");
    }
}

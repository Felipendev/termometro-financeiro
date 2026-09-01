package br.com.felipe.termometro.comparativo.domain;

import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * RN-30 — os valores de referência calibrados numa conversa real com o Felipe em 2026-08-28,
 * categoria por categoria, sobre os números reais do catálogo (nunca um valor de exemplo). Só os
 * três grupos abaixo têm meta: os demais aparecem no gráfico só com o ponto "atual" — sem "bom"
 * nem "ideal" fabricados para preencher espaço.
 */
public final class ReferenciaDoGrupo {

    private static final BigDecimal LIMITE_RUIM_SOBRE_BOM = new BigDecimal("1.25");

    private static final Map<GrupoDoComparativo, ReferenciaDoGrupo> REFERENCIAS = Map.of(
            GrupoDoComparativo.MORADIA, de("0.25", "0.22"),
            GrupoDoComparativo.ASSINATURAS, de("0.015", "0.008"),
            GrupoDoComparativo.ALIMENTACAO, de("0.07", "0.06"));

    private final Percentual bom;
    private final Percentual ideal;
    private final Percentual ruim;

    private ReferenciaDoGrupo(Percentual bom, Percentual ideal) {
        this.bom = bom;
        this.ideal = ideal;
        this.ruim = Percentual.deFracao(bom.fracao().multiply(LIMITE_RUIM_SOBRE_BOM));
    }

    private static ReferenciaDoGrupo de(String bom, String ideal) {
        return new ReferenciaDoGrupo(
                Percentual.deFracao(new BigDecimal(bom)), Percentual.deFracao(new BigDecimal(ideal)));
    }

    public static Optional<ReferenciaDoGrupo> para(GrupoDoComparativo grupo) {
        return Optional.ofNullable(REFERENCIAS.get(grupo));
    }

    public @Nullable Percentual bom() {
        return bom;
    }

    public @Nullable Percentual ideal() {
        return ideal;
    }

    /** Limite superior da faixa ruim da RN-14: 25% acima do valor considerado bom. */
    public @Nullable Percentual ruim() {
        return ruim;
    }
}

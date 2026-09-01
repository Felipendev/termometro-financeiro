package br.com.felipe.termometro.rollupanual.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;

/** {@code (entrada - saída) / entrada}, exatamente como a coluna % da aba Economia original. */
public final class CalculadoraDeTaxaDeEconomia {

    private CalculadoraDeTaxaDeEconomia() {
    }

    public static Percentual calcula(Dinheiro entrada, Dinheiro saida) {
        if (!entrada.ehPositivo()) {
            return Percentual.deFracao(BigDecimal.ZERO);
        }
        return entrada.subtrair(saida).sobre(entrada);
    }
}

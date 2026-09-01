package br.com.felipe.termometro.rollupanual.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/** RN-10 reduzida — a mesma leitura da aba "Economia" da planilha original, mês a mês. */
public record MesDoRollup(Competencia competencia, Dinheiro entrada, Dinheiro saida, Percentual taxaEconomia) {

    public MesDoRollup {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(entrada, "entrada não pode ser nula");
        Objects.requireNonNull(saida, "saída não pode ser nula");
        Objects.requireNonNull(taxaEconomia, "taxa de economia não pode ser nula");
    }
}

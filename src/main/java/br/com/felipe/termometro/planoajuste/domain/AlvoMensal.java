package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/**
 * O alvo de gasto de um único mês da rampa (RN-15).
 *
 * @param mes                índice 1-based dentro da rampa (mês 1, mês 2, ...) — não é uma
 *                            competência de calendário, é a posição na progressão
 * @param alvo                quanto a categoria pode gastar naquele mês
 * @param reducaoPercentual   a queda percentual em relação ao alvo do mês anterior (ou ao valor
 *                            atual, no mês 1) — constante ao longo de toda a rampa, por construção
 */
public record AlvoMensal(int mes, Dinheiro alvo, Percentual reducaoPercentual) {

    public AlvoMensal {
        Objects.requireNonNull(alvo, "alvo não pode ser nulo");
        Objects.requireNonNull(reducaoPercentual, "reducaoPercentual não pode ser nula");
        if (mes < 1) {
            throw new IllegalArgumentException("mes deve ser >= 1: " + mes);
        }
    }
}

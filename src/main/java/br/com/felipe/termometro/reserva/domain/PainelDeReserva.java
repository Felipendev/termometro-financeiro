package br.com.felipe.termometro.reserva.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @param proximoNivel o primeiro nível ainda não atingido, na ordem UM_MES → TRES_MESES →
 *                      SEIS_MESES; {@code null} apenas no caso teórico em que os três já estão
 *                      atingidos hoje
 */
public record PainelDeReserva(Dinheiro custoMensal, List<StatusDoNivel> niveis,
                               @Nullable NivelDeReserva proximoNivel) {

    public PainelDeReserva {
        Objects.requireNonNull(custoMensal, "custo mensal não pode ser nulo");
        Objects.requireNonNull(niveis, "níveis não podem ser nulos");
        niveis = List.copyOf(niveis);
    }
}

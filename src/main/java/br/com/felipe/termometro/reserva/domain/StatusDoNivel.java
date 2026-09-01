package br.com.felipe.termometro.reserva.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @param atingido            {@code true} somente quando o alvo já está coberto no primeiro mês
 *                             da simulação (hoje) — ver o Javadoc de
 *                             {@link CalculadoraDeNiveisDeReserva} sobre por que isso raramente
 *                             acontece na prática
 * @param competenciaPrevista primeiro mês simulado em que a reserva acumulada cobre o alvo;
 *                             {@code null} se o horizonte simulado terminar sem cobrir
 */
public record StatusDoNivel(NivelDeReserva nivel, Dinheiro alvo, boolean atingido,
                             @Nullable Competencia competenciaPrevista) {

    public StatusDoNivel {
        Objects.requireNonNull(nivel, "nível não pode ser nulo");
        Objects.requireNonNull(alvo, "alvo não pode ser nulo");
        if (atingido && competenciaPrevista == null) {
            throw new IllegalArgumentException("nível atingido precisa vir com a competência em que foi atingido");
        }
    }
}

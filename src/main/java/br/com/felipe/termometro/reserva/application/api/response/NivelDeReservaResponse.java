package br.com.felipe.termometro.reserva.application.api.response;

import br.com.felipe.termometro.reserva.domain.NivelDeReserva;
import br.com.felipe.termometro.reserva.domain.StatusDoNivel;
import br.com.felipe.termometro.shared.Dinheiro;

public record NivelDeReservaResponse(
        NivelDeReserva nivel,
        Dinheiro alvo,
        boolean atingido,
        String competenciaPrevista) {

    public NivelDeReservaResponse(StatusDoNivel status) {
        this(status.nivel(), status.alvo(), status.atingido(),
                status.competenciaPrevista() == null ? null : status.competenciaPrevista().toString());
    }
}

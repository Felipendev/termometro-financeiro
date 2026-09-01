package br.com.felipe.termometro.reserva.application.api.response;

import br.com.felipe.termometro.reserva.domain.NivelDeReserva;
import br.com.felipe.termometro.reserva.domain.PainelDeReserva;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;

public record PainelDeReservaResponse(
        Dinheiro custoMensal,
        List<NivelDeReservaResponse> niveis,
        NivelDeReserva proximoNivel) {

    public PainelDeReservaResponse(PainelDeReserva painel) {
        this(painel.custoMensal(), painel.niveis().stream().map(NivelDeReservaResponse::new).toList(),
                painel.proximoNivel());
    }
}

package br.com.felipe.termometro.catalogo.application.api.response;

import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.shared.Dinheiro;

public record PisoHumanoResponse(String categoria, Dinheiro valorPiso, String justificativa,
                                  boolean estimado) {

    public PisoHumanoResponse(PisoHumano piso) {
        this(piso.categoria(), piso.valorPiso(), piso.justificativa(), piso.estimado());
    }
}

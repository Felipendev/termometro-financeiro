package br.com.felipe.termometro.catalogo.application.api.response;

import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.UUID;

public record DividaResponse(UUID id, String nome, Dinheiro valorParcela,
                              String competenciaUltimaParcela, String observacao) {

    public DividaResponse(Divida divida) {
        this(divida.id(), divida.nome(), divida.valorParcela(),
                divida.competenciaUltimaParcela().toString(), divida.observacao());
    }
}

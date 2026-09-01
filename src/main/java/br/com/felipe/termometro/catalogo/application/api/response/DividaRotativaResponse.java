package br.com.felipe.termometro.catalogo.application.api.response;

import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.UUID;

public record DividaRotativaResponse(UUID id, String nome, Dinheiro saldoDevedor,
                                      Percentual taxaJurosMensal, boolean taxaEstimada, String observacao) {

    public DividaRotativaResponse(DividaRotativa dividaRotativa) {
        this(dividaRotativa.id(), dividaRotativa.nome(), dividaRotativa.saldoDevedor(),
                dividaRotativa.taxaJurosMensal(), dividaRotativa.taxaEstimada(), dividaRotativa.observacao());
    }
}

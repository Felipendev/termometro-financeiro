package br.com.felipe.termometro.diagnostico.application.api.response;

import br.com.felipe.termometro.diagnostico.domain.QuedaDeRenda;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;

public record QuedaDeRendaResponse(
        Dinheiro rendaAnterior,
        Dinheiro rendaAtual,
        Percentual quedaPct,
        Percentual pesoFixoAntes,
        Percentual pesoFixoAgora,
        Dinheiro excedenteEstrutural,
        String mensagem) {

    public QuedaDeRendaResponse(QuedaDeRenda quedaDeRenda) {
        this(quedaDeRenda.rendaAnterior(), quedaDeRenda.rendaAtual(), quedaDeRenda.quedaPct(),
                quedaDeRenda.pesoFixoAntes(), quedaDeRenda.pesoFixoAgora(),
                quedaDeRenda.excedenteEstrutural(), quedaDeRenda.mensagem());
    }
}

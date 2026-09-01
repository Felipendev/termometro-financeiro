package br.com.felipe.termometro.projecao.application.api.response;

import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.shared.Dinheiro;

public record MesProjetadoResponse(
        String competencia,
        Dinheiro entrada,
        Dinheiro saidaFixa,
        Dinheiro saidaVariavel,
        Dinheiro disponivel,
        Dinheiro juros,
        Dinheiro amortizacao,
        Dinheiro reservaAcumulada,
        Dinheiro saldoDividaFimDoMes,
        boolean apertado) {

    public MesProjetadoResponse(MesProjetado mes) {
        this(mes.competencia().toString(), mes.entrada(), mes.saidaFixa(), mes.saidaVariavel(),
                mes.disponivel(), mes.juros(), mes.amortizacao(), mes.reservaAcumulada(),
                mes.saldoDividaFimDoMes(), mes.apertado());
    }
}

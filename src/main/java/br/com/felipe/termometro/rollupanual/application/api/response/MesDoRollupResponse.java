package br.com.felipe.termometro.rollupanual.application.api.response;

import br.com.felipe.termometro.rollupanual.domain.MesDoRollup;

public record MesDoRollupResponse(String competencia, String entrada, String saida, String taxaEconomia) {

    public static MesDoRollupResponse de(MesDoRollup mes) {
        return new MesDoRollupResponse(
                mes.competencia().toString(), mes.entrada().paraJson(), mes.saida().paraJson(), mes.taxaEconomia().paraJson());
    }
}

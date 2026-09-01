package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planilha.domain.ItemDoDia;

public record LancamentoDaPlanilhaResponse(
        String id,
        String descricao,
        String valor,
        String tipo,
        String origem,
        String usoDeCredito,
        boolean editavel,
        String categoria,
        String grupoCategoria,
        String naturezaCategoria,
        String origemReceita) {

    public static LancamentoDaPlanilhaResponse de(ItemDoDia item) {
        return new LancamentoDaPlanilhaResponse(
                item.id() == null ? null : item.id().toString(), item.descricao(),
                item.valor().paraJson(), item.tipo().name(), item.origem(),
                item.usoDeCredito() == null ? null : item.usoDeCredito().name(), item.editavel(),
                item.categoria(), item.grupoCategoria(), item.naturezaCategoria(), item.origemReceita());
    }
}

package br.com.felipe.termometro.comparativo.application.api.response;

import br.com.felipe.termometro.comparativo.domain.PontoComparativo;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record PontoComparativoResponse(
        String grupo, String atual, @Nullable String bom, @Nullable String ideal, @Nullable String ruim,
        String valorAtual, String rendaReferencia, String fonte, List<ItemComparativoResponse> itens) {

    public static PontoComparativoResponse de(PontoComparativo ponto) {
        return new PontoComparativoResponse(
                ponto.grupo().name(),
                ponto.atual().paraJson(),
                ponto.bom() == null ? null : ponto.bom().paraJson(),
                ponto.ideal() == null ? null : ponto.ideal().paraJson(),
                ponto.ruim() == null ? null : ponto.ruim().paraJson(),
                ponto.valorAtual().paraJson(),
                ponto.rendaReferencia().paraJson(),
                ponto.fonte(),
                ponto.itens().stream().map(item -> new ItemComparativoResponse(
                        item.descricao(), item.categoria(), item.valor().paraJson(), item.origem())).toList());
    }

    public record ItemComparativoResponse(String descricao, String categoria, String valor, String origem) { }
}

package br.com.felipe.termometro.comparativo.application.api.response;

import br.com.felipe.termometro.comparativo.domain.PontoComparativo;
import org.jspecify.annotations.Nullable;

public record PontoComparativoResponse(
        String grupo, String atual, @Nullable String bom, @Nullable String ideal, @Nullable String ruim) {

    public static PontoComparativoResponse de(PontoComparativo ponto) {
        return new PontoComparativoResponse(
                ponto.grupo().name(),
                ponto.atual().paraJson(),
                ponto.bom() == null ? null : ponto.bom().paraJson(),
                ponto.ideal() == null ? null : ponto.ideal().paraJson(),
                ponto.ruim() == null ? null : ponto.ruim().paraJson());
    }
}

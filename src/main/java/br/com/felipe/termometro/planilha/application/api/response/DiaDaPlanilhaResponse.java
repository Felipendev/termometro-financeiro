package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planilha.domain.DiaDaPlanilha;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DiaDaPlanilhaResponse(
        LocalDate data,
        String entrada,
        String saida,
        String diario,
        boolean diarioSobrescrito,
        String saldo,
        String faixaSaldo,
        List<LancamentoDaPlanilhaResponse> lancamentos,
        @Nullable String observacao) {

    public static DiaDaPlanilhaResponse de(DiaDaPlanilha dia) {
        return new DiaDaPlanilhaResponse(
                dia.data(),
                dia.entrada().paraJson(),
                dia.saida().paraJson(),
                dia.diario().paraJson(),
                dia.diarioSobrescrito(),
                dia.saldo().paraJson(),
                dia.faixaSaldo().name(),
                dia.lancamentos().stream().map(LancamentoDaPlanilhaResponse::de).toList(),
                dia.observacao());
    }
}

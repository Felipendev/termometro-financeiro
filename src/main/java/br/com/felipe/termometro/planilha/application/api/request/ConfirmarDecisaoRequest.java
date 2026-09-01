package br.com.felipe.termometro.planilha.application.api.request;

import br.com.felipe.termometro.planilha.application.service.ComandoDeDecisao;
import br.com.felipe.termometro.planilha.domain.FormaPagamento;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ConfirmarDecisaoRequest(
        @NotNull LocalDate data,
        @NotNull String valor,
        @NotBlank String descricao,
        @NotNull FormaPagamento formaPagamento,
        int parcelas) {

    public ComandoDeDecisao paraComando() {
        return new ComandoDeDecisao(data, Dinheiro.de(valor), descricao, formaPagamento, Math.max(parcelas, 1));
    }
}

package br.com.felipe.termometro.catalogo.application.api.request;

import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Declara ou atualiza a renda líquida de uma competência (RN-17). Upsert — a competência vem do path. */
public record RendaRequest(
        @NotNull(message = "informe o valor líquido da renda")
        @PositiveOrZero(message = "a renda não pode ser negativa")
        BigDecimal valorLiquido,

        String observacao) {

    public Renda paraDominio(Competencia competencia) {
        return new Renda(competencia, Dinheiro.de(valorLiquido), observacao);
    }
}

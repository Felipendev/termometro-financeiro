package br.com.felipe.termometro.orcamento.application.api.request;

import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Define o teto de gasto variável de um mês.
 *
 * <p>Valores em {@link BigDecimal} para não perder centavo no caminho — {@code double} num campo de
 * dinheiro é bug esperando data.
 */
public record VerbaMensalRequest(
        @NotNull(message = "informe o limite mensal de gastos variáveis")
        @PositiveOrZero(message = "a verba não pode ser negativa")
        BigDecimal verbaVariavel,

        @NotNull(message = "informe a provisão para eventos")
        @PositiveOrZero(message = "a provisão não pode ser negativa")
        BigDecimal provisao) {

    public VerbaMensal paraDominio(Competencia competencia) {
        return new VerbaMensal(competencia, Dinheiro.de(verbaVariavel), Dinheiro.de(provisao));
    }
}

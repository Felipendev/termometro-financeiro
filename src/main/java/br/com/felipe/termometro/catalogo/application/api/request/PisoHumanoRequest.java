package br.com.felipe.termometro.catalogo.application.api.request;

import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Cria ou atualiza o piso humano de uma categoria (RN-05, RN-08). Upsert — a categoria vem do path. */
public record PisoHumanoRequest(
        @NotNull(message = "informe o valor do piso")
        @PositiveOrZero(message = "o piso não pode ser negativo")
        BigDecimal valorPiso,

        String justificativa,

        @NotNull(message = "informe se o piso é estimado") Boolean estimado) {

    public PisoHumano paraDominio(String categoria) {
        return new PisoHumano(categoria, Dinheiro.de(valorPiso), justificativa, estimado);
    }
}

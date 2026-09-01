package br.com.felipe.termometro.catalogo.application.api.request;

import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cria ou atualiza um item de custo fixo (RN-08, RN-16). Upsert — o id vem do path, gerado no
 * cliente para um item novo. {@code ativo} é {@link Boolean} boxed, não {@code boolean}
 * primitivo: se o campo sumir do corpo da requisição, {@code @NotNull} rejeita em vez de assumir
 * silenciosamente {@code false}.
 */
public record CustoFixoItemRequest(
        @NotBlank(message = "informe o nome do item") String nome,

        @NotNull(message = "informe o valor do item")
        @PositiveOrZero(message = "o valor não pode ser negativo")
        BigDecimal valor,

        String formaPagamento,

        String observacao,

        @NotNull(message = "informe se o item está ativo") Boolean ativo) {

    public CustoFixoItem paraDominio(UUID id) {
        return new CustoFixoItem(id, nome, Dinheiro.de(valor), formaPagamento, observacao, ativo);
    }
}

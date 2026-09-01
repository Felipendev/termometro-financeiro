package br.com.felipe.termometro.cartao.application.api.request;

import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cria ou atualiza um cartão cadastrado à mão. Upsert — o id vem do path, gerado no cliente para
 * um cartão novo (mesmo padrão de {@code DividaRequest}/{@code CustoFixoItemRequest}).
 *
 */
public record CartaoRequest(
        @NotBlank(message = "informe o nome do cartão") String nome,

        @PositiveOrZero(message = "o limite não pode ser negativo")
        BigDecimal limite,

        @NotNull(message = "informe o valor da fatura")
        @PositiveOrZero(message = "o valor da fatura não pode ser negativo")
        BigDecimal valorFatura,

        String observacao) {

    public Cartao paraDominio(UUID id) {
        return new Cartao(id, nome, limite == null ? null : Dinheiro.de(limite),
                Dinheiro.de(valorFatura), observacao, true);
    }
}

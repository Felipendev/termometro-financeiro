package br.com.felipe.termometro.catalogo.application.api.request;

import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cria ou atualiza um saldo rotativo (RN-09). Upsert — o id vem do path. Quitar de verdade é só
 * editar {@code saldoDevedor} para 0 — some sozinho de {@code buscaDividasRotativasAtivas}
 * (filtro {@code saldoDevedor > 0}), sem precisar remover a linha.
 */
public record DividaRotativaRequest(
        @NotBlank(message = "informe o nome da dívida") String nome,

        @NotNull(message = "informe o saldo devedor")
        @PositiveOrZero(message = "o saldo devedor não pode ser negativo")
        BigDecimal saldoDevedor,

        @NotNull(message = "informe a taxa de juros mensal")
        @PositiveOrZero(message = "a taxa de juros não pode ser negativa")
        BigDecimal taxaJurosMensal,

        @NotNull(message = "informe se a taxa é estimada") Boolean taxaEstimada,

        String observacao) {

    public DividaRotativa paraDominio(UUID id) {
        return new DividaRotativa(id, nome, Dinheiro.de(saldoDevedor),
                Percentual.deFracao(taxaJurosMensal), taxaEstimada, observacao);
    }
}

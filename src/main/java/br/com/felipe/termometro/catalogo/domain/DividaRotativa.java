package br.com.felipe.termometro.catalogo.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;
import java.util.UUID;

/**
 * Saldo rotativo — dívida sem parcela nem prazo fixos, só um saldo e uma taxa (RN-09: é o que o
 * motor de projeção efetivamente simula pagando mês a mês). Diferente de {@link Divida}, que
 * modela parcela fixa com prazo já conhecido e não depende de nenhuma estratégia de amortização.
 *
 * @param taxaEstimada {@code true} quando a taxa não veio medida de uma fatura real (edge case 8)
 */
public record DividaRotativa(UUID id, String nome, Dinheiro saldoDevedor, Percentual taxaJurosMensal,
                              boolean taxaEstimada, String observacao) {

    public DividaRotativa {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(saldoDevedor, "saldo devedor não pode ser nulo");
        Objects.requireNonNull(taxaJurosMensal, "taxa de juros mensal não pode ser nula");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (saldoDevedor.ehNegativo()) {
            throw new IllegalArgumentException("saldo devedor não pode ser negativo: " + saldoDevedor);
        }
        if (taxaJurosMensal.ehNegativo()) {
            throw new IllegalArgumentException("taxa de juros mensal não pode ser negativa: " + taxaJurosMensal);
        }
    }
}

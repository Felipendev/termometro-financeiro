package br.com.felipe.termometro.catalogo.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/**
 * Um item do custo fixo mensal (aluguel, imposto, assinaturas...). A soma dos itens ativos é o
 * {@code CustoFixoTotal} da RN-08 e da RN-16.
 */
public record CustoFixoItem(UUID id, String nome, Dinheiro valor, String formaPagamento,
                             String observacao, boolean ativo) {

    public CustoFixoItem {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (valor.ehNegativo()) {
            throw new IllegalArgumentException("valor não pode ser negativo: " + valor);
        }
    }
}

package br.com.felipe.termometro.cartao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Um cartão cadastrado à mão, com o valor da fatura que Felipe digitou — estado atual, sem
 * histórico por competência (mesmo espírito de {@code DividaRotativa.saldoDevedor}: um valor que
 * se edita quando muda, não uma série mensal). Fatura muda todo mês; é responsabilidade de quem
 * usa o cadastro atualizar o valor quando a fatura fechar.
 *
 * @param limite                  {@code null} se não quiser declarar limite
 */
public record Cartao(
        UUID id,
        String nome,
        @Nullable Dinheiro limite,
        Dinheiro valorFatura,
        @Nullable String observacao,
        boolean ativo) {

    public Cartao {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(valorFatura, "valor da fatura não pode ser nulo");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (limite != null && limite.ehNegativo()) {
            throw new IllegalArgumentException("limite não pode ser negativo: " + limite);
        }
        if (valorFatura.ehNegativo()) {
            throw new IllegalArgumentException("valor da fatura não pode ser negativo: " + valorFatura);
        }
    }

    public Optional<Dinheiro> limiteOpcional() {
        return Optional.ofNullable(limite);
    }
}

package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma das três ações de maior retorno sobre dor (RN-15): {@code impacto = economiaMensal / dor}.
 * {@code descricao} é a frase pronta em linguagem direta que a spec pede na resposta do
 * {@code GET /plano-ajuste}.
 *
 * <p>{@code impacto} é um fator de ranqueamento, não dinheiro — por isso é {@link BigDecimal}
 * com 6 casas, e não {@link Dinheiro}, que arredondaria para centavos e poderia empatar ações que
 * na verdade têm ordens de prioridade diferentes.
 */
public record AcaoPrioritaria(
        String categoria, String descricao, Dinheiro economiaMensal, int dor, BigDecimal impacto) {

    public AcaoPrioritaria {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(descricao, "descricao não pode ser nula");
        Objects.requireNonNull(economiaMensal, "economiaMensal não pode ser nulo");
        Objects.requireNonNull(impacto, "impacto não pode ser nulo");
        if (dor < 1) {
            throw new IllegalArgumentException("dor deve ser >= 1: " + dor);
        }
    }
}

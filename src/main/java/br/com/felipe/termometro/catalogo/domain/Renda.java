package br.com.felipe.termometro.catalogo.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * Renda líquida declarada para uma competência (RN-16.1 precisa da série histórica; hoje é uma
 * série de um ponto só — R$ 10.000 fixo, sem variação declarada).
 */
public record Renda(Competencia competencia, Dinheiro valorLiquido, String observacao) {

    public Renda {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(valorLiquido, "valor líquido não pode ser nulo");
        if (valorLiquido.ehNegativo()) {
            throw new IllegalArgumentException("renda não pode ser negativa: " + valorLiquido);
        }
    }
}

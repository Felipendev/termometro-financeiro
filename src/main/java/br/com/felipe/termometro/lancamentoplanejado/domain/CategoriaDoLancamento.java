package br.com.felipe.termometro.lancamentoplanejado.domain;

import java.util.Objects;

/** Categoria escolhida no momento do lançamento, antes de existir uma regra automática. */
public record CategoriaDoLancamento(String nome, String grupo, String natureza) {
    public CategoriaDoLancamento {
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(grupo, "grupo não pode ser nulo");
        Objects.requireNonNull(natureza, "natureza não pode ser nula");
        if (nome.isBlank() || grupo.isBlank() || natureza.isBlank()) {
            throw new IllegalArgumentException("categoria, grupo e natureza são obrigatórios");
        }
    }
}

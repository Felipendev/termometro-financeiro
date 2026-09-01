package br.com.felipe.termometro.classificacao.domain;

import java.util.Objects;

/**
 * Categoria de uma transação.
 *
 * @param nome     identificador estável, usado em regra e em relatório
 * @param grupo    agrupamento para o semáforo (RN-14)
 * @param natureza define se entra ou não na verba diária (RN-19)
 */
public record Categoria(String nome, GrupoDeCategoria grupo, Natureza natureza) {

    public Categoria {
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(grupo, "grupo não pode ser nulo");
        Objects.requireNonNull(natureza, "natureza não pode ser nula");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome da categoria não pode ser vazio");
        }
    }

    /** Categoria de quem o sistema não conseguiu classificar — vai para a fila da RN-12. */
    public static final Categoria NAO_IDENTIFICADA =
            new Categoria("NAO_IDENTIFICADA", GrupoDeCategoria.OUTROS, Natureza.VARIAVEL);

    public boolean ehNaoIdentificada() {
        return NAO_IDENTIFICADA.nome().equals(nome);
    }
}

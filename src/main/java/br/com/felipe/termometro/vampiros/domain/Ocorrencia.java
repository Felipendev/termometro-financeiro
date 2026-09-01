package br.com.felipe.termometro.vampiros.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Uma cobrança individual dentro de um grupo já identificado como o mesmo estabelecimento
 * (RN-02/RN-07: {@code Normalizador.chaveDeEstabelecimento}). {@code valor} é a magnitude da
 * cobrança — positivo mesmo sendo despesa (RN-01 guarda o sinal na transação, não aqui; quem
 * monta a lista já extraiu o módulo).
 */
public record Ocorrencia(LocalDate data, Dinheiro valor) {

    public Ocorrencia {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor da ocorrência deve ser positivo: " + valor);
        }
    }
}

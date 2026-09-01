package br.com.felipe.termometro.catalogo.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * O mínimo mensal que você declarou não conseguir cortar numa categoria (RN-05, RN-08). A soma de
 * todos os pisos é o {@code PisoVariavelTotal} da RN-16.
 *
 * @param estimado {@code true} quando o piso não foi declarado e foi inferido (percentil 25 do
 *                 histórico, RN-08) — nunca fica implícito num agregado sem essa marca.
 */
public record PisoHumano(String categoria, Dinheiro valorPiso, String justificativa, boolean estimado) {

    public PisoHumano {
        Objects.requireNonNull(valorPiso, "valor do piso não pode ser nulo");
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("categoria não pode ser vazia");
        }
        if (valorPiso.ehNegativo()) {
            throw new IllegalArgumentException("piso não pode ser negativo: " + valorPiso);
        }
    }
}

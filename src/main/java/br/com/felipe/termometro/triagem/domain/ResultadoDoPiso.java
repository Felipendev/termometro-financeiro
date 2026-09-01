package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/**
 * A etiqueta que a transação recebe (valor inteiro, para persistir) e a divisão lógica do seu
 * valor entre azul e amarelo (só para o agregado — RN-05 é explícita que isso não altera o
 * registro). Para qualquer transação que não seja a que cruza o piso, uma das duas partes é zero.
 */
public record ResultadoDoPiso(UUID transacaoId, Etiqueta etiqueta, Dinheiro parteAzul, Dinheiro parteAmarela) {

    public ResultadoDoPiso {
        Objects.requireNonNull(transacaoId, "transacaoId não pode ser nulo");
        Objects.requireNonNull(etiqueta, "etiqueta não pode ser nula");
        Objects.requireNonNull(parteAzul, "parteAzul não pode ser nula");
        Objects.requireNonNull(parteAmarela, "parteAmarela não pode ser nula");
    }
}

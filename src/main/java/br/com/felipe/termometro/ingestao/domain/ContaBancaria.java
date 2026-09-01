package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Conta em uma instituição, como o agregador a entrega.
 *
 * @param idExterno        identificador da conta no provedor
 * @param identificador    chave estável usada na deduplicação (RN-02) e nas consultas
 * @param limite           limite total, quando é cartão de crédito
 */
public record ContaBancaria(
        String idExterno,
        String identificador,
        String nome,
        TipoDeConta tipo,
        @Nullable String numeroMascarado,
        Dinheiro saldo,
        @Nullable Dinheiro limite) {

    public ContaBancaria {
        Objects.requireNonNull(idExterno, "id externo não pode ser nulo");
        Objects.requireNonNull(identificador, "identificador não pode ser nulo");
        Objects.requireNonNull(nome, "nome não pode ser nulo");
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(saldo, "saldo não pode ser nulo");
    }

    public Optional<Dinheiro> limiteOpcional() {
        return Optional.ofNullable(limite);
    }

    public boolean ehCartaoDeCredito() {
        return tipo.ehCartaoDeCredito();
    }
}

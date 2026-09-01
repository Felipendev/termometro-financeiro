package br.com.felipe.termometro.contamanual.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/** Conta digitada pelo usuário, independente das contas que a sincronização externa atualiza. */
public record ContaManual(UUID id, String identificador, String nome, TipoContaManual tipo,
                          Dinheiro saldo, boolean ativa) {
    public ContaManual {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(saldo, "saldo não pode ser nulo");
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("identificador não pode ser vazio");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
    }

    public ContaManual creditar(Dinheiro valor) {
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("crédito deve ser positivo");
        }
        return comSaldo(saldo.somar(valor));
    }

    public ContaManual debitar(Dinheiro valor) {
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("débito deve ser positivo");
        }
        return comSaldo(saldo.subtrair(valor));
    }

    private ContaManual comSaldo(Dinheiro novoSaldo) {
        return new ContaManual(id, identificador, nome, tipo, novoSaldo, ativa);
    }
}

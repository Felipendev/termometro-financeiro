package br.com.felipe.termometro.planilha.application.service;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;

public record ComandoDeDecisao(
        LocalDate data, Dinheiro valor, String descricao, br.com.felipe.termometro.planilha.domain.FormaPagamento formaPagamento, int parcelas) {

    public ComandoDeDecisao {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(formaPagamento, "forma de pagamento não pode ser nula");
        if (parcelas < 1) {
            throw new IllegalArgumentException("parcelas deve ser ao menos 1");
        }
    }
}

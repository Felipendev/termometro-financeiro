package br.com.felipe.termometro.cartao.fatura.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PagamentoFatura(UUID id, String referenciaCartao, String nomeCartao,
        Competencia competencia, Dinheiro valor, LocalDate dataPagamento,
        UUID lancamentoPlanejadoId) {
    public PagamentoFatura {
        Objects.requireNonNull(id);
        Objects.requireNonNull(referenciaCartao);
        Objects.requireNonNull(nomeCartao);
        Objects.requireNonNull(competencia);
        Objects.requireNonNull(valor);
        Objects.requireNonNull(dataPagamento);
        Objects.requireNonNull(lancamentoPlanejadoId);
        if (!valor.ehPositivo()) throw new IllegalArgumentException("valor do pagamento deve ser positivo");
        if (!competencia.contem(dataPagamento)) {
            throw new IllegalArgumentException("pagamento deve pertencer à competência da fatura");
        }
    }
}

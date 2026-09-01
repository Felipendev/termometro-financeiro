package br.com.felipe.termometro.orcamento.application.api.request;

import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Agenda um gasto irregular conhecido: a viagem de sábado, o evento do mês que vem (RN-20). */
public record EventoRequest(
        @NotNull(message = "informe a data do evento") LocalDate data,

        @NotBlank(message = "descreva o evento")
        @Size(max = 120, message = "a descrição deve ter no máximo {max} caracteres")
        String descricao,

        @NotNull(message = "informe o valor do evento")
        @Positive(message = "o valor do evento deve ser positivo")
        BigDecimal valor) {

    public Evento paraDominio() {
        return Evento.previsto(data, descricao, Dinheiro.de(valor));
    }
}

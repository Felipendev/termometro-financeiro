package br.com.felipe.termometro.cartao.fatura.application.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PagamentoFaturaRequest(
        @NotBlank String referencia,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate dataPagamento,
        UUID contaOrigemId) { }

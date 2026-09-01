package br.com.felipe.termometro.cartao.fatura.application.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ValorFaturaDeclaradaRequest(
        @NotBlank String referencia,
        @NotNull @PositiveOrZero BigDecimal valor) { }

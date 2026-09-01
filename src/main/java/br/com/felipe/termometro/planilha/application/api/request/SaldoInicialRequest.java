package br.com.felipe.termometro.planilha.application.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SaldoInicialRequest(@NotNull LocalDate dataReferencia, @NotNull String valor) {
}

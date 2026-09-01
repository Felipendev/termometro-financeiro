package br.com.felipe.termometro.planilha.application.api.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DiarioSerieRequest(@NotNull LocalDate de, @NotNull LocalDate ate, @NotNull String valor) {
}

package br.com.felipe.termometro.planilha.application.api.request;

import jakarta.validation.constraints.NotNull;

public record DiarioRequest(@NotNull String valor) {
}

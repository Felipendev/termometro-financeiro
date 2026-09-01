package br.com.felipe.termometro.planilha.application.api.request;

import jakarta.validation.constraints.NotBlank;

public record ObservacaoRequest(@NotBlank String texto) {
}

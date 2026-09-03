package br.com.felipe.termometro.auth.application.api.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {}

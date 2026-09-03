package br.com.felipe.termometro.auth.application.api.response;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm) {}

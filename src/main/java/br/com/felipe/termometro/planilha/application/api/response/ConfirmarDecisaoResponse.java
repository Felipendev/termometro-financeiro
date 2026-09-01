package br.com.felipe.termometro.planilha.application.api.response;

import java.util.List;
import java.util.UUID;

public record ConfirmarDecisaoResponse(List<UUID> lancamentosCriados) {
}

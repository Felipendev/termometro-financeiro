package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.contamanual.application.api.response.ContaManualResponse;
import br.com.felipe.termometro.lancamentoplanejado.application.api.response.LancamentoPlanejadoResponse;
import java.util.List;

/** Agregado operacional da home; preserva os Três Eus como detalhe analítico. */
public record DashboardInicioResponse(DashboardResponse analise, List<ContaManualResponse> contasManuais,
                                      List<LancamentoPlanejadoResponse> pendencias) {}

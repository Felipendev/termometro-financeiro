package br.com.felipe.termometro.dashboard.application.service;

import br.com.felipe.termometro.contamanual.application.api.response.ContaManualResponse;
import br.com.felipe.termometro.contamanual.application.service.ContaManualApplicationService;
import br.com.felipe.termometro.dashboard.application.api.response.DashboardInicioResponse;
import br.com.felipe.termometro.lancamentoplanejado.application.api.response.LancamentoPlanejadoResponse;
import br.com.felipe.termometro.lancamentoplanejado.application.service.LancamentoPlanejadoApplicationService;
import br.com.felipe.termometro.shared.Competencia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class DashboardInicioApplicationService {
 private final DashboardService dashboardService; private final ContaManualApplicationService contas; private final LancamentoPlanejadoApplicationService lancamentos;
 public DashboardInicioResponse monta(Competencia competencia){return new DashboardInicioResponse(dashboardService.monta(competencia),contas.listaAtivas().stream().map(ContaManualResponse::new).toList(),lancamentos.listaPendentes().stream().map(LancamentoPlanejadoResponse::new).toList());}
}

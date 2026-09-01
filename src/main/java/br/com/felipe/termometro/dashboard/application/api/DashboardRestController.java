package br.com.felipe.termometro.dashboard.application.api;

import br.com.felipe.termometro.dashboard.application.api.response.DashboardResponse;
import br.com.felipe.termometro.dashboard.application.service.DashboardService;
import br.com.felipe.termometro.dashboard.application.service.DashboardInicioApplicationService;
import br.com.felipe.termometro.dashboard.application.api.response.DashboardInicioResponse;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class DashboardRestController implements DashboardAPI {

    private final DashboardService dashboardService;
    private final DashboardInicioApplicationService dashboardInicioService;

    @Override
    public DashboardResponse getDashboard(String competencia) {
        log.info("[inicia] DashboardRestController - getDashboard [{}]", competencia);
        DashboardResponse resposta = dashboardService.monta(competenciaDe(competencia));
        log.info("[finaliza] DashboardRestController - getDashboard [{}]", competencia);
        return resposta;
    }

    @Override
    public DashboardInicioResponse getInicio(String competencia) {
        return dashboardInicioService.monta(competenciaDe(competencia));
    }

    private Competencia competenciaDe(String competencia) {
        try {
            return Competencia.parse(competencia);
        } catch (DateTimeParseException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Competência inválida: '" + competencia + "'. Use o formato AAAA-MM.", e);
        }
    }
}

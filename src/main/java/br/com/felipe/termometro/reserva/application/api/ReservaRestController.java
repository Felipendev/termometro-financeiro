package br.com.felipe.termometro.reserva.application.api;

import br.com.felipe.termometro.reserva.application.api.response.PainelDeReservaResponse;
import br.com.felipe.termometro.reserva.application.service.ReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ReservaRestController implements ReservaAPI {

    private final ReservaService reservaService;

    @Override
    public PainelDeReservaResponse getPainel() {
        log.info("[inicia] ReservaRestController - getPainel");
        PainelDeReservaResponse resposta = new PainelDeReservaResponse(reservaService.consultaPainel());
        log.info("[finaliza] ReservaRestController - getPainel");
        return resposta;
    }
}

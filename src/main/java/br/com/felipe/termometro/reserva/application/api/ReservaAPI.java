package br.com.felipe.termometro.reserva.application.api;

import br.com.felipe.termometro.reserva.application.api.response.PainelDeReservaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST da RN-21. A implementação fica no {@code RestController}. */
@RestController
@RequestMapping("/v1/reserva")
public interface ReservaAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    PainelDeReservaResponse getPainel();
}

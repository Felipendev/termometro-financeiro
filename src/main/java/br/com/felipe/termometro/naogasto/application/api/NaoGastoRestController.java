package br.com.felipe.termometro.naogasto.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.naogasto.application.api.response.ResultadoDaConciliacaoResponse;
import br.com.felipe.termometro.naogasto.application.service.NaoGastoService;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class NaoGastoRestController implements NaoGastoAPI {

    private final NaoGastoService naoGastoService;

    @Override
    public ResultadoDaConciliacaoResponse concilia(String competencia) {
        return ResultadoDaConciliacaoResponse.de(naoGastoService.concilia(competenciaDe(competencia)));
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

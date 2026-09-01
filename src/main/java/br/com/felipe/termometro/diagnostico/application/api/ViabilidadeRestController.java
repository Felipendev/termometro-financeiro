package br.com.felipe.termometro.diagnostico.application.api;

import br.com.felipe.termometro.diagnostico.application.api.response.ViabilidadeResponse;
import br.com.felipe.termometro.diagnostico.application.service.ViabilidadeService;
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
public class ViabilidadeRestController implements ViabilidadeAPI {

    private final ViabilidadeService viabilidadeService;

    @Override
    public ViabilidadeResponse getViabilidade(String competencia) {
        log.info("[inicia] ViabilidadeRestController - getViabilidade");
        ViabilidadeResponse resposta =
                new ViabilidadeResponse(viabilidadeService.consultaViabilidade(competenciaDe(competencia)));
        log.info("[finaliza] ViabilidadeRestController - getViabilidade");
        return resposta;
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

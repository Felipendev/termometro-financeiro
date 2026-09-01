package br.com.felipe.termometro.vampiros.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.vampiros.application.api.response.RecorrenciaResponse;
import br.com.felipe.termometro.vampiros.application.service.VampirosService;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class VampirosRestController implements VampirosAPI {

    private final VampirosService vampirosService;

    @Override
    public List<RecorrenciaResponse> getVampiros(String competencia) {
        log.info("[inicia] VampirosRestController - getVampiros");
        List<RecorrenciaResponse> resposta = vampirosService.listaVampiros(competenciaDe(competencia)).stream()
                .map(RecorrenciaResponse::new)
                .toList();
        log.info("[finaliza] VampirosRestController - getVampiros [{}]", resposta.size());
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

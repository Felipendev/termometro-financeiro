package br.com.felipe.termometro.diagnostico.application.api;

import br.com.felipe.termometro.diagnostico.application.api.response.SaldoDeSobrevivenciaResponse;
import br.com.felipe.termometro.diagnostico.application.service.DiagnosticoService;
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
public class DiagnosticoRestController implements DiagnosticoAPI {

    private final DiagnosticoService diagnosticoService;

    @Override
    public SaldoDeSobrevivenciaResponse getSaldoDeSobrevivencia(String competencia) {
        log.info("[inicia] DiagnosticoRestController - getSaldoDeSobrevivencia");
        SaldoDeSobrevivenciaResponse resposta = new SaldoDeSobrevivenciaResponse(
                diagnosticoService.consultaSaldoDeSobrevivencia(competenciaDe(competencia)));
        log.info("[finaliza] DiagnosticoRestController - getSaldoDeSobrevivencia");
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

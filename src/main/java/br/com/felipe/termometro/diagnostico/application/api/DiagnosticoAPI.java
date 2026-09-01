package br.com.felipe.termometro.diagnostico.application.api;

import br.com.felipe.termometro.diagnostico.application.api.response.SaldoDeSobrevivenciaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST da RN-08. A implementação fica no {@code RestController}. */
@RestController
@RequestMapping("/v1/diagnostico")
public interface DiagnosticoAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    SaldoDeSobrevivenciaResponse getSaldoDeSobrevivencia(@RequestParam String competencia);
}

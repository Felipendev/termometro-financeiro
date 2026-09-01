package br.com.felipe.termometro.diagnostico.application.api;

import br.com.felipe.termometro.diagnostico.application.api.response.ViabilidadeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST da RN-16. A implementação fica no {@code RestController}. */
@RestController
@RequestMapping("/v1/viabilidade")
public interface ViabilidadeAPI {

    /** A pergunta central: dá para bater a meta com o padrão de vida atual, ou ele precisa cair? */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ViabilidadeResponse getViabilidade(@RequestParam String competencia);
}

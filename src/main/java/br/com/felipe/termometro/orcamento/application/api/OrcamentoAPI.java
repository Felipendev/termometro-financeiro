package br.com.felipe.termometro.orcamento.application.api;

import br.com.felipe.termometro.orcamento.application.api.request.EventoRequest;
import br.com.felipe.termometro.orcamento.application.api.request.VerbaMensalRequest;
import br.com.felipe.termometro.orcamento.application.api.response.EventoResponse;
import br.com.felipe.termometro.orcamento.application.api.response.VerbaDoDiaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contrato REST do orçamento. A implementação fica no {@code RestController}. */
@RestController
@RequestMapping("/v1/orcamento")
public interface OrcamentoAPI {

    /** A tela única do MVP: quanto dá para gastar hoje. */
    @GetMapping("/hoje")
    @ResponseStatus(HttpStatus.OK)
    VerbaDoDiaResponse getVerbaDeHoje();

    @GetMapping("/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    VerbaDoDiaResponse getVerbaDoMes(@PathVariable String competencia);

    @PutMapping("/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    VerbaDoDiaResponse putVerbaDoMes(@PathVariable String competencia,
                                     @RequestBody @Valid VerbaMensalRequest request);

    @PostMapping("/{competencia}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    EventoResponse postEvento(@PathVariable String competencia,
                              @RequestBody @Valid EventoRequest request);
}

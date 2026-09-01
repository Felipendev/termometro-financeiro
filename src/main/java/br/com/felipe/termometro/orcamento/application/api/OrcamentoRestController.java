package br.com.felipe.termometro.orcamento.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.orcamento.application.api.request.EventoRequest;
import br.com.felipe.termometro.orcamento.application.api.request.VerbaMensalRequest;
import br.com.felipe.termometro.orcamento.application.api.response.EventoResponse;
import br.com.felipe.termometro.orcamento.application.api.response.VerbaDoDiaResponse;
import br.com.felipe.termometro.orcamento.application.service.OrcamentoService;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OrcamentoRestController implements OrcamentoAPI {

    private final OrcamentoService orcamentoService;

    @Override
    public VerbaDoDiaResponse getVerbaDeHoje() {
        log.info("[inicia] OrcamentoRestController - getVerbaDeHoje");
        VerbaDoDiaResponse resposta = new VerbaDoDiaResponse(orcamentoService.consultaVerbaDeHoje());
        log.info("[finaliza] OrcamentoRestController - getVerbaDeHoje");
        return resposta;
    }

    @Override
    public VerbaDoDiaResponse getVerbaDoMes(String competencia) {
        log.info("[inicia] OrcamentoRestController - getVerbaDoMes");
        VerbaDoDiaResponse resposta =
                new VerbaDoDiaResponse(orcamentoService.consultaVerbaDoMes(competenciaDe(competencia)));
        log.info("[finaliza] OrcamentoRestController - getVerbaDoMes");
        return resposta;
    }

    @Override
    public VerbaDoDiaResponse putVerbaDoMes(String competencia, VerbaMensalRequest request) {
        log.info("[inicia] OrcamentoRestController - putVerbaDoMes");
        Competencia mes = competenciaDe(competencia);
        orcamentoService.defineVerbaDoMes(mes, request);
        VerbaDoDiaResponse resposta = new VerbaDoDiaResponse(orcamentoService.consultaVerbaDoMes(mes));
        log.info("[finaliza] OrcamentoRestController - putVerbaDoMes");
        return resposta;
    }

    @Override
    public EventoResponse postEvento(String competencia, EventoRequest request) {
        log.info("[inicia] OrcamentoRestController - postEvento");
        EventoResponse resposta =
                new EventoResponse(orcamentoService.agendaEvento(competenciaDe(competencia), request));
        log.info("[finaliza] OrcamentoRestController - postEvento");
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

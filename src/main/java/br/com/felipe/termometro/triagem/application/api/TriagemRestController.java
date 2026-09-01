package br.com.felipe.termometro.triagem.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.triagem.application.service.TriagemService;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TriagemRestController implements TriagemAPI {

    private final TriagemService triagemService;

    @Override
    public ResultadoDaTriagemResponse executa(String competencia) {
        log.info("[inicia] TriagemRestController - executa [{}]", competencia);
        ResultadoDaTriagemResponse resposta = triagemService.executaTriagem(competenciaDe(competencia));
        log.info("[finaliza] TriagemRestController - executa [{}]", competencia);
        return resposta;
    }

    @Override
    public List<ResumoDeCategoriaResponse> resumo(String competencia) {
        log.info("[inicia] TriagemRestController - resumo [{}]", competencia);
        List<ResumoDeCategoriaResponse> resposta = triagemService.resumo(competenciaDe(competencia));
        log.info("[finaliza] TriagemRestController - resumo [{}]", competencia);
        return resposta;
    }

    @Override
    public void promoveParaVermelha(UUID id) {
        log.info("[inicia] TriagemRestController - promoveParaVermelha [{}]", id);
        triagemService.promoveParaVermelha(id);
        log.info("[finaliza] TriagemRestController - promoveParaVermelha [{}]", id);
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

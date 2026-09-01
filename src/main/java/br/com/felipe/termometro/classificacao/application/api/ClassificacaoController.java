package br.com.felipe.termometro.classificacao.application.api;

import br.com.felipe.termometro.classificacao.application.api.request.ClassificarTransacaoRequest;
import br.com.felipe.termometro.classificacao.application.api.response.ContextoDeRevisaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaCorrecaoResponse;
import java.util.List;
import java.util.UUID;
import br.com.felipe.termometro.classificacao.application.service.ClassificacaoService;
import br.com.felipe.termometro.shared.Competencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ClassificacaoController implements ClassificacaoAPI {

    private final ClassificacaoService classificacaoService;

    @Override
    public ResultadoDaClassificacaoResponse classifica(String competencia) {
        log.info("[inicia] ClassificacaoController - classifica");
        ResultadoDaClassificacaoResponse resposta =
                classificacaoService.classifica(Competencia.parse(competencia));
        log.info("[finaliza] ClassificacaoController - classifica");
        return resposta;
    }

    @Override
    public ResultadoDaClassificacaoResponse reclassifica(String competencia) {
        log.info("[inicia] ClassificacaoController - reclassifica");
        ResultadoDaClassificacaoResponse resposta =
                classificacaoService.reclassifica(Competencia.parse(competencia));
        log.info("[finaliza] ClassificacaoController - reclassifica");
        return resposta;
    }

    @Override
    public List<ContextoDeRevisaoResponse> filaDeRevisao(String competencia, int limite) {
        log.info("[inicia] ClassificacaoController - filaDeRevisao");
        List<ContextoDeRevisaoResponse> fila =
                classificacaoService.filaDeRevisao(Competencia.parse(competencia), limite);
        log.info("[finaliza] ClassificacaoController - filaDeRevisao");
        return fila;
    }

    @Override
    public ResultadoDaCorrecaoResponse corrige(UUID id, ClassificarTransacaoRequest request) {
        log.info("[inicia] ClassificacaoController - corrige");
        ResultadoDaCorrecaoResponse resposta = classificacaoService.corrige(id, request);
        log.info("[finaliza] ClassificacaoController - corrige");
        return resposta;
    }
}

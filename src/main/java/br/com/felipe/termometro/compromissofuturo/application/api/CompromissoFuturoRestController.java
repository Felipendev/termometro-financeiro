package br.com.felipe.termometro.compromissofuturo.application.api;

import br.com.felipe.termometro.compromissofuturo.application.api.response.ResultadoDaGeracaoResponse;
import br.com.felipe.termometro.compromissofuturo.application.service.CompromissoFuturoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CompromissoFuturoRestController implements CompromissoFuturoAPI {

    private final CompromissoFuturoService compromissoFuturoService;

    @Override
    public ResultadoDaGeracaoResponse gera() {
        return ResultadoDaGeracaoResponse.de(compromissoFuturoService.gera());
    }
}

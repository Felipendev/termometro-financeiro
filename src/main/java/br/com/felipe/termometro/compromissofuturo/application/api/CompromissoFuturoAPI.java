package br.com.felipe.termometro.compromissofuturo.application.api;

import br.com.felipe.termometro.compromissofuturo.application.api.response.ResultadoDaGeracaoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RN-04 — compromissos futuros: as parcelas de cartão ainda não sincronizadas de uma compra
 * parcelada já contratada. Sem competência no caminho — a geração recalcula todas as séries de
 * uma vez (ver Javadoc de {@code CompromissoFuturoApplicationService}), diferente de
 * {@code POST /v1/nao-gasto/{competencia}}, que é por competência.
 */
@RestController
@RequestMapping("/v1/compromissos-futuros")
public interface CompromissoFuturoAPI {

    @PostMapping("/gerar")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaGeracaoResponse gera();
}

package br.com.felipe.termometro.projecao.application.api;

import br.com.felipe.termometro.projecao.application.api.response.ProjecaoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST da RN-09. A spec original sugere {@code POST /cenarios/{id}/projetar}, mas este
 * código ainda não tem o conceito de {@code cenario} persistido — a estratégia e o horizonte vêm
 * direto como parâmetro, no mesmo espírito de leitura direta que {@code /v1/diagnostico} e
 * {@code /v1/viabilidade}. Quando "cenário" virar algo que se salva e se compara, isto migra
 * para o contrato da spec.
 */
@RestController
@RequestMapping("/v1/projecao")
public interface ProjecaoAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ProjecaoResponse getProjecao(@RequestParam String competencia,
            @RequestParam(defaultValue = "AVALANCHE") String estrategia,
            @RequestParam(defaultValue = "60") int horizonteMeses);
}

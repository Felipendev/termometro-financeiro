package br.com.felipe.termometro.sistema.application.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ponto de verificação usado pela interface antes de enviar qualquer contrato de escrita. */
@RestController
@RequestMapping("/v1/sistema")
public class CompatibilidadeRestController {

    public static final String CONTRATO_API = "2026-09-01-planilha-editavel-v1";

    @GetMapping("/compatibilidade")
    public CompatibilidadeResponse compatibilidade() {
        return new CompatibilidadeResponse(CONTRATO_API);
    }

    public record CompatibilidadeResponse(String contratoApi) {
    }
}

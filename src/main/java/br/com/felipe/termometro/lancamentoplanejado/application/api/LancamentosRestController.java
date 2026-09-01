package br.com.felipe.termometro.lancamentoplanejado.application.api;

import br.com.felipe.termometro.lancamentoplanejado.application.api.response.ConsultaLancamentosResponse;
import br.com.felipe.termometro.lancamentoplanejado.application.service.ConsultaLancamentosService;
import br.com.felipe.termometro.shared.Competencia;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/lancamentos")
@RequiredArgsConstructor
public class LancamentosRestController {
    private final ConsultaLancamentosService service;

    @GetMapping
    public ConsultaLancamentosResponse consulta(
            @RequestParam String competencia,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID contaId,
            @RequestParam(required = false) UUID cartaoId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, name = "q") String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "30") int tamanho) {
        return new ConsultaLancamentosResponse(service.consulta(new ConsultaLancamentosService.Filtro(
                Competencia.parse(competencia), tipo, status,
                contaId, cartaoId, categoria, texto, pagina, tamanho)));
    }
}

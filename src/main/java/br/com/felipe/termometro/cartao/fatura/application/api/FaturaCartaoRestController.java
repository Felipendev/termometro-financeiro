package br.com.felipe.termometro.cartao.fatura.application.api;

import br.com.felipe.termometro.cartao.fatura.application.api.request.PagamentoFaturaRequest;
import br.com.felipe.termometro.cartao.fatura.application.api.request.ValorFaturaDeclaradaRequest;
import br.com.felipe.termometro.cartao.fatura.application.api.response.FaturaCartaoResponse;
import br.com.felipe.termometro.cartao.fatura.application.service.FaturaCartaoApplicationService;
import br.com.felipe.termometro.shared.Competencia;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/faturas-cartao")
@RequiredArgsConstructor
public class FaturaCartaoRestController {
    private final FaturaCartaoApplicationService service;

    @GetMapping
    public List<FaturaCartaoResponse> consulta(@RequestParam String competencia) {
        return service.consulta(Competencia.parse(competencia));
    }

    @PostMapping("/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public FaturaCartaoResponse paga(@RequestParam String competencia,
            @RequestBody @Valid PagamentoFaturaRequest request) {
        return service.paga(Competencia.parse(competencia), request);
    }

    @org.springframework.web.bind.annotation.PutMapping("/valor-declarado")
    public FaturaCartaoResponse declara(@RequestParam String competencia,
            @RequestBody @Valid ValorFaturaDeclaradaRequest request) {
        return service.declara(Competencia.parse(competencia), request);
    }
}

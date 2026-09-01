package br.com.felipe.termometro.contribuicao.application.api;
import br.com.felipe.termometro.contribuicao.application.api.response.*;import java.util.*;import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/metas-contribuicao") public interface ContribuicaoAPI {
    @GetMapping List<MetaContribuicaoResponse> consulta(@RequestParam String competencia);
    @PostMapping("/{nome}/autorizar-proximo-passo") MetaContribuicaoResponse autoriza(@PathVariable String nome, @RequestParam String competencia);
}

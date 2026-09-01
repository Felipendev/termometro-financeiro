package br.com.felipe.termometro.triagem.application.api;

import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/triagem")
public interface TriagemAPI {

    /** Roda o algoritmo do piso (RN-05) nas transações já classificadas do mês. */
    @PostMapping("/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaTriagemResponse executa(@PathVariable @NotBlank String competencia);

    /** Totais por categoria e cor, recalculados na leitura — não persistidos. */
    @GetMapping("/{competencia}/resumo")
    @ResponseStatus(HttpStatus.OK)
    List<ResumoDeCategoriaResponse> resumo(@PathVariable @NotBlank String competencia);

    /** Promove manualmente uma transação hoje AMARELA para VERMELHA (impulso reconhecido). */
    @PostMapping("/transacoes/{id}/promover-vermelha")
    @ResponseStatus(HttpStatus.OK)
    void promoveParaVermelha(@PathVariable UUID id);
}

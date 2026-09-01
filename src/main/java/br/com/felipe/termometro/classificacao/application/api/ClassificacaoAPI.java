package br.com.felipe.termometro.classificacao.application.api;

import br.com.felipe.termometro.classificacao.application.api.request.ClassificarTransacaoRequest;
import br.com.felipe.termometro.classificacao.application.api.response.ContextoDeRevisaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaCorrecaoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface ClassificacaoAPI {

    /** Classifica o que ainda não foi classificado no mês. */
    @PostMapping("/classificacao/{competencia}")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaClassificacaoResponse classifica(@PathVariable @NotBlank String competencia);

    /**
     * Reclassifica o mês inteiro. Chamada depois que o usuário cria ou corrige uma regra — a
     * decisão dele precisa valer para o passado, não só daqui para frente (RN-17).
     */
    @PostMapping("/classificacao/{competencia}/reclassificar")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaClassificacaoResponse reclassifica(@PathVariable @NotBlank String competencia);

    /** Fila da RN-12: o que o sistema não decidiu, com o contexto para você decidir. */
    @GetMapping("/transacoes/nao-identificadas")
    @ResponseStatus(HttpStatus.OK)
    List<ContextoDeRevisaoResponse> filaDeRevisao(@RequestParam @NotBlank String competencia,
                                                  @RequestParam(defaultValue = "20") int limite);

    /** Corrige uma transação — e, com {@code aplicarAoGrupo}, ensina o sistema de vez. */
    @PostMapping("/transacoes/{id}/classificar")
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaCorrecaoResponse corrige(@PathVariable UUID id,
                                        @RequestBody @Valid ClassificarTransacaoRequest request);
}

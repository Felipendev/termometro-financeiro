package br.com.felipe.termometro.vampiros.application.api;

import br.com.felipe.termometro.vampiros.application.api.response.RecorrenciaResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato REST da RN-07. Só leitura por ora — {@code PATCH /vampiros/{id}} da spec depende de
 * persistir a recorrência com decisão do usuário, que esta fatia não faz (ver Javadoc de
 * {@code VampirosApplicationService}).
 */
@RestController
@RequestMapping("/v1/vampiros")
public interface VampirosAPI {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<RecorrenciaResponse> getVampiros(@RequestParam String competencia);
}

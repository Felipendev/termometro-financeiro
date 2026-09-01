package br.com.felipe.termometro.contamanual.application.api;
import br.com.felipe.termometro.contamanual.application.api.request.ContaManualRequest;
import br.com.felipe.termometro.contamanual.application.api.response.ContaManualResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/contas-manuais") public interface ContaManualAPI {
 @GetMapping List<ContaManualResponse> lista();
 @PutMapping("/{id}") ContaManualResponse salva(@PathVariable UUID id,@RequestBody @Valid ContaManualRequest request);
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void remove(@PathVariable UUID id);
}

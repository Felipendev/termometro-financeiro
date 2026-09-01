package br.com.felipe.termometro.comparativo.application.api;
import br.com.felipe.termometro.comparativo.application.api.response.*;import java.util.*;import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/visao-geral/comparativo-categorias") public interface ComparativoAPI {
    @GetMapping List<PontoComparativoResponse> consulta(@RequestParam String competencia);
}

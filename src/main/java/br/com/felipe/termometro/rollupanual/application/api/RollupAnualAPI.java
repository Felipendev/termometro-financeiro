package br.com.felipe.termometro.rollupanual.application.api;
import br.com.felipe.termometro.rollupanual.application.api.response.*;import java.util.*;import org.springframework.web.bind.annotation.*;
@RequestMapping("/v1/relatorios/rollup-anual") public interface RollupAnualAPI {
    @GetMapping List<MesDoRollupResponse> consulta(@RequestParam int ano);
}

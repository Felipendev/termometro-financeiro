package br.com.felipe.termometro.rollupanual.application.api;

import br.com.felipe.termometro.rollupanual.application.api.response.MesDoRollupResponse;
import br.com.felipe.termometro.rollupanual.application.service.RollupAnualService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RollupAnualRestController implements RollupAnualAPI {

    private final RollupAnualService rollupAnualService;

    @Override
    public List<MesDoRollupResponse> consulta(int ano) {
        return rollupAnualService.consulta(ano).stream().map(MesDoRollupResponse::de).toList();
    }
}

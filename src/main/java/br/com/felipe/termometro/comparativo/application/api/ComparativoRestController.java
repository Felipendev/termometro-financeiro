package br.com.felipe.termometro.comparativo.application.api;

import br.com.felipe.termometro.comparativo.application.api.response.PontoComparativoResponse;
import br.com.felipe.termometro.comparativo.application.service.ComparativoService;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ComparativoRestController implements ComparativoAPI {

    private final ComparativoService comparativoService;

    @Override
    public List<PontoComparativoResponse> consulta(String competencia) {
        return comparativoService.consulta(Competencia.parse(competencia)).stream()
                .map(PontoComparativoResponse::de)
                .toList();
    }
}

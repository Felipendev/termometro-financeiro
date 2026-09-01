package br.com.felipe.termometro.comparativo.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.comparativo.domain.CalculadoraDoComparativo;
import br.com.felipe.termometro.comparativo.domain.PontoComparativo;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * RN-30 — pura composição sobre o catálogo já existente, mesmo espírito de {@code planoajuste} e
 * {@code reserva}: nenhuma tabela nova, o gráfico é uma leitura sobre premissas que a Fase 4/5 já
 * declarava para RN-08/RN-16.
 */
@Service
@RequiredArgsConstructor
public class ComparativoApplicationService implements ComparativoService {

    private final CatalogoRepository catalogoRepository;

    @Override
    public List<PontoComparativo> consulta(Competencia competencia) {
        Dinheiro rendaLiquida = catalogoRepository.buscaRenda(competencia)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma renda declarada para " + competencia + "."))
                .valorLiquido();

        Map<String, Dinheiro> valoresPorNomeDeItem = new HashMap<>();
        for (CustoFixoItem item : catalogoRepository.buscaCustoFixoAtivo()) {
            valoresPorNomeDeItem.put(item.nome(), item.valor());
        }
        for (PisoHumano piso : catalogoRepository.buscaPisoHumano()) {
            valoresPorNomeDeItem.put(piso.categoria(), piso.valorPiso());
        }

        return CalculadoraDoComparativo.calcula(valoresPorNomeDeItem, rendaLiquida);
    }
}

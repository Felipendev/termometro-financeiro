package br.com.felipe.termometro.comparativo.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.comparativo.domain.CalculadoraDoComparativo;
import br.com.felipe.termometro.comparativo.domain.PontoComparativo;
import br.com.felipe.termometro.comparativo.domain.ItemComparativo;
import br.com.felipe.termometro.comparativo.domain.MapeadorDeGrupo;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RN-30 — lançamentos manuais e importados do mês são a fonte principal. O catálogo antigo só
 * entra quando ainda não existe nenhuma despesa real classificada na competência.
 */
@Service
@RequiredArgsConstructor
public class ComparativoApplicationService implements ComparativoService {

    private final CatalogoRepository catalogoRepository;
    private final LancamentoPlanejadoRepository planejados;
    private final LancamentoImportadoRepository importados;

    @Override
    public List<PontoComparativo> consulta(Competencia competencia) {
        var manuais = planejados.buscaPorCompetencia(competencia).stream()
                .filter(item -> item.status() != StatusLancamentoPlanejado.CANCELADO)
                .toList();
        var movimentosImportados = importados.buscaPorCompetencia(competencia);
        Dinheiro rendaReal = Dinheiro.somaDe(java.util.stream.Stream.concat(
                manuais.stream()
                        .filter(item -> item.tipo() == TipoLancamentoPlanejado.RECEITA)
                        .map(item -> item.valor()),
                movimentosImportados.stream()
                        .filter(item -> item.valorComSinal().ehPositivo())
                        .filter(item -> !"NAO_E_GASTO".equals(item.natureza()))
                        .filter(item -> !"TRANSFERENCIA".equals(item.grupo()))
                        .map(item -> item.valorComSinal()))
                .toList());
        Dinheiro rendaLiquida = rendaReal.ehPositivo() ? rendaReal
                : catalogoRepository.buscaRenda(competencia)
                        .map(renda -> renda.valorLiquido())
                        .orElse(Dinheiro.ZERO);

        List<ItemComparativo> reais = java.util.stream.Stream.concat(
                manuais.stream()
                        .filter(item -> item.tipo() == TipoLancamentoPlanejado.DESPESA)
                        .filter(item -> item.categoria() != null)
                        .filter(item -> !"NAO_E_GASTO".equals(item.categoria().natureza()))
                        .map(item -> new ItemComparativo(
                                MapeadorDeGrupo.grupoDe(item.categoria().grupo(), item.categoria().nome()),
                                item.descricao(), item.categoria().nome(), item.valor(), "MANUAL")),
                movimentosImportados.stream()
                        .filter(item -> item.valorComSinal().ehNegativo())
                        .filter(item -> item.categoria() != null && !item.categoria().isBlank())
                        .filter(item -> !"NAO_E_GASTO".equals(item.natureza()))
                        .filter(item -> !"TRANSFERENCIA".equals(item.grupo()))
                        .map(item -> new ItemComparativo(
                                MapeadorDeGrupo.grupoDe(item.grupo(), item.categoria()),
                                item.descricao(), item.categoria(), item.valorComSinal().absoluto(),
                                item.origem())))
                .toList();

        if (!reais.isEmpty()) {
            return CalculadoraDoComparativo.calcula(reais, rendaLiquida, "LANCAMENTOS_DO_MES");
        }

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

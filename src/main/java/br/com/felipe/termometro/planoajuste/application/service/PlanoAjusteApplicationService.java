package br.com.felipe.termometro.planoajuste.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.planoajuste.domain.GastoDaCategoria;
import br.com.felipe.termometro.planoajuste.domain.MotorDoPlanoDeAjuste;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.repository.TriagemRepository;
import br.com.felipe.termometro.triagem.domain.MotorDeTriagem;
import br.com.felipe.termometro.triagem.domain.ResumoDeCategoria;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RN-15. Não tem {@code infra} própria — compõe direto as portas já existentes de
 * {@code triagem} e {@code catalogo}, o mesmo padrão de dependência entre módulos usado por
 * {@code vampiros} sobre {@code ingestao}. Reaproveita {@link MotorDeTriagem#resumir} (RN-05)
 * para obter azul/amarelo/vermelho por categoria em cada um dos últimos meses fechados, em vez de
 * duplicar essa lógica de divisão-lógica-por-piso aqui.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanoAjusteApplicationService implements PlanoAjusteService {

    /** RN-15: "atual_c = mediana dos últimos 3 meses fechados". */
    static final int MESES_DE_HISTORICO = 3;

    private final TriagemRepository triagemRepository;
    private final CatalogoRepository catalogoRepository;

    @Override
    public PlanoDeAjuste gera(Competencia referencia, int mesesRampaSolicitados, BigDecimal fatorMaxCorte) {
        log.info("[inicia] gera plano de ajuste referencia={} mesesRampaSolicitados={} fatorMaxCorte={}",
                referencia, mesesRampaSolicitados, fatorMaxCorte);

        Map<String, Dinheiro> pisoPorCategoria = new HashMap<>();
        for (PisoHumano piso : catalogoRepository.buscaPisoHumano()) {
            pisoPorCategoria.put(piso.categoria(), piso.valorPiso());
        }

        Competencia ultimoFechado = referencia.menos(1);
        Competencia inicioDoHistorico = ultimoFechado.menos(MESES_DE_HISTORICO - 1);
        List<Competencia> mesesFechados = inicioDoHistorico.ate(ultimoFechado).toList();

        // categoria -> lista de (azul+amarelo) e lista de vermelho, um valor por mês do histórico,
        // na mesma ordem de mesesFechados — só categorias VARIAVEL entram: FIXO e NAO_E_GASTO não
        // rampam (RN-15 só fala de "categoria variável")
        Map<String, List<Dinheiro>> variavelPorCategoria = new HashMap<>();
        Map<String, List<Dinheiro>> vermelhoPorCategoria = new HashMap<>();

        for (Competencia mes : mesesFechados) {
            List<TransacaoClassificada> doMes = triagemRepository.buscaClassificadasDoMes(mes);
            List<ResumoDeCategoria> resumos = MotorDeTriagem.resumir(doMes, pisoPorCategoria);

            for (ResumoDeCategoria resumo : resumos) {
                if (resumo.natureza() != Natureza.VARIAVEL) {
                    continue;
                }
                variavelPorCategoria
                        .computeIfAbsent(resumo.categoria(), c -> new ArrayList<>())
                        .add(resumo.totalAzul().somar(resumo.totalAmarelo()));
                vermelhoPorCategoria
                        .computeIfAbsent(resumo.categoria(), c -> new ArrayList<>())
                        .add(resumo.totalVermelho());
            }
        }

        // Mantém os meses com vermelho = ZERO na lista (não filtra): a mediana precisa refletir o
        // histórico inteiro da categoria, não só os meses em que houve impulso — senão um único mês
        // com vermelho alto vira "mediano" mesmo que os outros dois meses não tivessem nenhum.
        List<GastoDaCategoria> categorias = new ArrayList<>();
        for (String categoria : variavelPorCategoria.keySet()) {
            List<Dinheiro> vermelhos = vermelhoPorCategoria.getOrDefault(categoria, List.of());
            categorias.add(new GastoDaCategoria(categoria, variavelPorCategoria.get(categoria), vermelhos,
                    pisoPorCategoria.get(categoria)));
        }

        PlanoDeAjuste plano =
                MotorDoPlanoDeAjuste.gerar(referencia, categorias, fatorMaxCorte, mesesRampaSolicitados);

        log.info("[finaliza] gera plano de ajuste referencia={} itens={} avisos={}", referencia,
                plano.itens().size(), plano.avisos().size());
        return plano;
    }
}

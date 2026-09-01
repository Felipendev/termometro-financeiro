package br.com.felipe.termometro.diagnostico.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.diagnostico.domain.TesteDeViabilidade;
import br.com.felipe.termometro.diagnostico.domain.Viabilidade;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-16: busca as premissas no catálogo (renda, custo fixo, piso humano), soma o
 * que o domínio só sabe somar, e delega o veredito para {@link TesteDeViabilidade}.
 *
 * <p>Depende de {@code CatalogoRepository} — a porta de saída do módulo {@code catalogo} — não da
 * sua implementação em Postgres. É a única costura entre os dois módulos.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViabilidadeApplicationService implements ViabilidadeService {

    /** Meses de histórico buscados para a detecção de queda estrutural (RN-16.1: precisa de 6). */
    private static final int MESES_DE_HISTORICO_DE_RENDA = 6;

    private final CatalogoRepository catalogoRepository;
    private final TotaisMarcadosDoMes totaisMarcados;

    @Override
    public Viabilidade consultaViabilidade(Competencia competencia) {
        log.info("[inicia] ViabilidadeApplicationService - consultaViabilidade");

        Renda renda = catalogoRepository.buscaRenda(competencia)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma renda declarada para " + competencia + "."));

        Dinheiro custoFixoTotal = totaisMarcados.marcadoOuLegado(competencia,
                MarcacaoPlanejamento.CUSTO_FIXO, Dinheiro.somaDe(
                        catalogoRepository.buscaCustoFixoAtivo().stream().map(CustoFixoItem::valor).toList()));
        Dinheiro pisoVariavelTotal = totaisMarcados.marcadoOuLegado(competencia,
                MarcacaoPlanejamento.PISO_HUMANO, Dinheiro.somaDe(
                        catalogoRepository.buscaPisoHumano().stream().map(PisoHumano::valorPiso).toList()));
        List<Renda> historicoDeRenda =
                catalogoRepository.buscaHistoricoDeRenda(competencia, MESES_DE_HISTORICO_DE_RENDA);

        Viabilidade viabilidade = TesteDeViabilidade.calcular(competencia, renda.valorLiquido(),
                custoFixoTotal, pisoVariavelTotal, metaEconomia(), historicoDeRenda);

        log.info("[finaliza] ViabilidadeApplicationService - consultaViabilidade [{}]",
                viabilidade.veredito());
        return viabilidade;
    }

    /**
     * A meta declarada por Felipe: 25% a 30% da renda. Fixa em código por ora — quando a fatia
     * de configuração de metas por usuário existir, isto migra para lá.
     */
    private Percentual metaEconomia() {
        return Percentual.dePontos("25");
    }
}

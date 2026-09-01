package br.com.felipe.termometro.diagnostico.application.service;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.diagnostico.domain.CalculadoraDeSaldoDeSobrevivencia;
import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-08: soma o catálogo (custo fixo, piso, dívidas) com o que a ingestão já sabe
 * sobre parcelas do mês, e delega a conta para {@link CalculadoraDeSaldoDeSobrevivencia}.
 *
 * <p>Depende das portas de dois módulos — {@code catalogo} e {@code ingestao} — nunca das suas
 * implementações de infra. É a mesma costura da {@code ViabilidadeApplicationService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiagnosticoApplicationService implements DiagnosticoService {

    private final CatalogoRepository catalogoRepository;
    private final TransacaoRepository transacaoRepository;
    private final CompromissoFuturoRepository compromissoFuturoRepository;
    private final TotaisMarcadosDoMes totaisMarcados;

    @Override
    public SaldoDeSobrevivencia consultaSaldoDeSobrevivencia(Competencia competencia) {
        log.info("[inicia] DiagnosticoApplicationService - consultaSaldoDeSobrevivencia");

        Renda renda = catalogoRepository.buscaRenda(competencia)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma renda declarada para " + competencia + "."));

        Dinheiro custoFixoTotal = totaisMarcados.marcadoOuLegado(competencia,
                MarcacaoPlanejamento.CUSTO_FIXO, Dinheiro.somaDe(
                        catalogoRepository.buscaCustoFixoAtivo().stream().map(CustoFixoItem::valor).toList()));
        Dinheiro pisoVariavelTotal = totaisMarcados.marcadoOuLegado(competencia,
                MarcacaoPlanejamento.PISO_HUMANO, Dinheiro.somaDe(
                        catalogoRepository.buscaPisoHumano().stream().map(PisoHumano::valorPiso).toList()));
        Dinheiro servicoDivida = Dinheiro.somaDe(
                catalogoRepository.buscaDividasAtivas(competencia).stream().map(Divida::valorParcela).toList());
        Dinheiro compromissosFuturosDoMes = compromissosFuturosDoMes(competencia);

        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(competencia,
                renda.valorLiquido(), custoFixoTotal, compromissosFuturosDoMes, pisoVariavelTotal,
                servicoDivida);

        log.info("[finaliza] DiagnosticoApplicationService - consultaSaldoDeSobrevivencia [deficit={}]",
                saldo.deficit());
        return saldo;
    }

    /**
     * RN-04: soma dois pedaços que nunca se sobrepõem — a transação parcelada real já
     * sincronizada para este mês (fonte mais confiável, existe por construção) mais o
     * {@link CompromissoFuturo} gerado para a parcela deste mês que ainda não chegou como
     * transação. A reconciliação de {@code CompromissoFuturoApplicationService} apaga o
     * compromisso sintético assim que a parcela real correspondente é sincronizada — por isso
     * somar os dois aqui não conta a mesma parcela duas vezes, desde que a geração tenha rodado
     * depois do último sync (a mesma dependência de ordem que a RN-03 já tem: os números só
     * ficam corretos depois de {@code POST /v1/compromissos-futuros/gerar}).
     */
    private Dinheiro compromissosFuturosDoMes(Competencia competencia) {
        Dinheiro parcelasJaSincronizadas = Dinheiro.somaDe(
                transacaoRepository.buscaPorCompetencia(competencia).stream()
                        .filter(t -> t.parcelaOpcional().isPresent())
                        .map(TransacaoBruta::valor)
                        .map(Dinheiro::absoluto)
                        .toList());
        Dinheiro compromissosGerados = Dinheiro.somaDe(
                compromissoFuturoRepository.buscaPorPeriodo(competencia, competencia).stream()
                        .map(CompromissoFuturo::valor)
                        .toList());
        return parcelasJaSincronizadas.somar(compromissosGerados);
    }
}

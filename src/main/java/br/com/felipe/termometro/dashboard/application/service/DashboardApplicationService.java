package br.com.felipe.termometro.dashboard.application.service;

import br.com.felipe.termometro.cartao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.catalogo.application.api.response.DividaResponse;
import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.dashboard.application.api.response.CompromissoFuturoItemResponse;
import br.com.felipe.termometro.dashboard.application.api.response.DashboardResponse;
import br.com.felipe.termometro.dashboard.application.api.response.EuDoFuturoResponse;
import br.com.felipe.termometro.dashboard.application.api.response.EuDoPassadoResponse;
import br.com.felipe.termometro.dashboard.application.api.response.EuDoPresenteResponse;
import br.com.felipe.termometro.diagnostico.application.api.response.SaldoDeSobrevivenciaResponse;
import br.com.felipe.termometro.diagnostico.application.api.response.ViabilidadeResponse;
import br.com.felipe.termometro.diagnostico.application.service.DiagnosticoService;
import br.com.felipe.termometro.diagnostico.application.service.ViabilidadeService;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.service.CartaoService;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.planoajuste.application.api.response.PlanoDeAjusteResponse;
import br.com.felipe.termometro.planoajuste.application.service.PlanoAjusteService;
import br.com.felipe.termometro.projecao.application.api.response.MarcosResponse;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.reserva.application.api.response.PainelDeReservaResponse;
import br.com.felipe.termometro.reserva.application.service.ReservaService;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.triagem.application.service.TriagemService;
import br.com.felipe.termometro.vampiros.application.api.response.RecorrenciaResponse;
import br.com.felipe.termometro.vampiros.application.service.VampirosService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Composição da RN-11 (visão agregada): nenhuma chamada aqui recalcula nada que os outros módulos
 * já não calculem — este serviço só busca e empacota.
 *
 * <p><b>Horizonte de compromissos futuros fixo em 12 meses</b> (a competência pedida + 11):
 * mostrar tudo até a última parcela (parcelamentos vão até 48x) inundaria a coluna "Eu do
 * Passado" — decisão documentada, não vem da spec.
 *
 * <p><b>Plano de ajuste com os mesmos defaults do {@code GET /v1/plano-ajuste}</b> (3 meses de
 * rampa, corte máximo de 35%) — o dashboard não expõe controle desses parâmetros, é leitura, não
 * simulação interativa.
 *
 * <p><b>Precondição que este serviço não resolve:</b> classificação, triagem, não-gasto e
 * compromissos futuros precisam ter rodado para a competência pedida antes desta chamada (mesma
 * cadeia de dependência já documentada em {@code SincronizacaoApplicationService} e {@code
 * NotificacaoMatinalScheduler}). Este é um {@code GET} — não dispara esses passos como efeito
 * colateral; se o mês não foi processado, o resumo de triagem volta zerado/NAO_TRIADA, não é bug.
 *
 * <p><b>{@code cartoesManuais} lido direto de {@code CartaoRepository}</b> (módulo {@code cartao},
 * cadastro manual/stopgap), não de {@code CartaoService}/{@code ResumoCartoesResponse}
 * (automático) — os dois convivem sem se misturar; ver Javadoc de {@code EuDoPresenteResponse}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardApplicationService implements DashboardService {

    private static final int HORIZONTE_COMPROMISSOS_FUTUROS_MESES = 11;
    private static final int HORIZONTE_PROJECAO_MESES = 60;
    private static final int MESES_RAMPA_PADRAO = 3;
    private static final BigDecimal FATOR_MAX_CORTE_PADRAO = new BigDecimal("0.35");

    private final ViabilidadeService viabilidadeService;
    private final DiagnosticoService diagnosticoService;
    private final TriagemService triagemService;
    private final VampirosService vampirosService;
    private final ReservaService reservaService;
    private final PlanoAjusteService planoAjusteService;
    private final ProjecaoService projecaoService;
    private final CompromissoFuturoRepository compromissoFuturoRepository;
    private final CatalogoRepository catalogoRepository;
    private final CartaoService cartaoService;
    private final CartaoRepository cartaoRepository;

    @Override
    public DashboardResponse monta(Competencia competencia) {
        log.info("[inicia] DashboardApplicationService - monta [{}]", competencia);

        ViabilidadeResponse viabilidade =
                new ViabilidadeResponse(viabilidadeService.consultaViabilidade(competencia));
        EuDoPassadoResponse euDoPassado = montaEuDoPassado(competencia);
        EuDoPresenteResponse euDoPresente = montaEuDoPresente(competencia);
        EuDoFuturoResponse euDoFuturo = montaEuDoFuturo(competencia);

        DashboardResponse dashboard = new DashboardResponse(
                competencia.toString(), viabilidade, euDoPassado, euDoPresente, euDoFuturo);

        log.info("[finaliza] DashboardApplicationService - monta [{}]", competencia);
        return dashboard;
    }

    private EuDoPassadoResponse montaEuDoPassado(Competencia competencia) {
        List<CompromissoFuturo> compromissos = compromissoFuturoRepository.buscaPorPeriodo(
                competencia, competencia.mais(HORIZONTE_COMPROMISSOS_FUTUROS_MESES));
        List<CompromissoFuturoItemResponse> compromissosResponse =
                compromissos.stream().map(CompromissoFuturoItemResponse::new).toList();

        List<Divida> dividas = catalogoRepository.buscaDividasAtivas(competencia);
        List<DividaResponse> dividasResponse = dividas.stream().map(DividaResponse::new).toList();

        return new EuDoPassadoResponse(compromissosResponse, dividasResponse);
    }

    private EuDoPresenteResponse montaEuDoPresente(Competencia competencia) {
        SaldoDeSobrevivenciaResponse diagnostico =
                new SaldoDeSobrevivenciaResponse(diagnosticoService.consultaSaldoDeSobrevivencia(competencia));
        List<ResumoDeCategoriaResponse> resumoTriagem = triagemService.resumo(competencia);
        List<RecorrenciaResponse> vampiros =
                vampirosService.listaVampiros(competencia).stream().map(RecorrenciaResponse::new).toList();
        ResumoCartoesResponse cartoes = cartaoService.consultaCartoes(competencia);
        List<CartaoResponse> cartoesManuais =
                cartaoRepository.buscaAtivos().stream().map(CartaoResponse::new).toList();

        return new EuDoPresenteResponse(diagnostico, resumoTriagem, vampiros, cartoes, cartoesManuais);
    }

    private EuDoFuturoResponse montaEuDoFuturo(Competencia competencia) {
        MarcosResponse marcos = new MarcosResponse(
                projecaoService.projeta(competencia, Estrategia.AVALANCHE, HORIZONTE_PROJECAO_MESES).marcos());
        PainelDeReservaResponse reserva = null;
        String reservaIndisponivel = null;
        try {
            reserva = new PainelDeReservaResponse(reservaService.consultaPainel());
        } catch (APIException excecao) {
            if (excecao.getStatusException() != HttpStatus.NOT_FOUND) {
                throw excecao;
            }
            reservaIndisponivel = excecao.getMessage();
            log.info("Reserva indisponível no dashboard: {}", reservaIndisponivel);
        }
        PlanoDeAjusteResponse planoAjuste = PlanoDeAjusteResponse.de(
                planoAjusteService.gera(competencia, MESES_RAMPA_PADRAO, FATOR_MAX_CORTE_PADRAO));

        return new EuDoFuturoResponse(marcos, reserva, reservaIndisponivel, planoAjuste);
    }
}

package br.com.felipe.termometro.dashboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.cartao.application.repository.CartaoRepository;
import br.com.felipe.termometro.cartao.domain.Cartao;
import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.dashboard.application.api.response.DashboardResponse;
import br.com.felipe.termometro.diagnostico.application.service.DiagnosticoService;
import br.com.felipe.termometro.diagnostico.application.service.ViabilidadeService;
import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.diagnostico.domain.Veredito;
import br.com.felipe.termometro.diagnostico.domain.Viabilidade;
import br.com.felipe.termometro.ingestao.application.api.response.CartaoResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.service.CartaoService;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.planoajuste.application.service.PlanoAjusteService;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Marcos;
import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.projecao.domain.StatusProjecao;
import br.com.felipe.termometro.reserva.application.service.ReservaService;
import br.com.felipe.termometro.reserva.domain.NivelDeReserva;
import br.com.felipe.termometro.reserva.domain.PainelDeReserva;
import br.com.felipe.termometro.reserva.domain.StatusDoNivel;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.triagem.application.service.TriagemService;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.vampiros.application.service.VampirosService;
import br.com.felipe.termometro.vampiros.domain.Periodicidade;
import br.com.felipe.termometro.vampiros.domain.Recorrencia;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardApplicationService")
class DashboardApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private ViabilidadeService viabilidadeService;

    @Mock
    private DiagnosticoService diagnosticoService;

    @Mock
    private TriagemService triagemService;

    @Mock
    private VampirosService vampirosService;

    @Mock
    private ReservaService reservaService;

    @Mock
    private PlanoAjusteService planoAjusteService;

    @Mock
    private ProjecaoService projecaoService;

    @Mock
    private CompromissoFuturoRepository compromissoFuturoRepository;

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private CartaoService cartaoService;

    @Mock
    private CartaoRepository cartaoRepository;

    @Test
    @DisplayName("compõe as três colunas mais o veredito de viabilidade, sem recalcular nada")
    void componhaAsTresColunas() {
        when(viabilidadeService.consultaViabilidade(SETEMBRO)).thenReturn(viabilidade());
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO.mais(11)))
                .thenReturn(List.of(compromisso()));
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO)).thenReturn(List.of(divida()));
        when(diagnosticoService.consultaSaldoDeSobrevivencia(SETEMBRO)).thenReturn(saldo());
        when(triagemService.resumo(SETEMBRO)).thenReturn(List.of(
                new ResumoDeCategoriaResponse("RESTAURANTE", Natureza.VARIAVEL.name(),
                        Dinheiro.de("160.00"), Dinheiro.de("80.00"), Dinheiro.ZERO, Dinheiro.ZERO,
                        Dinheiro.ZERO)));
        when(vampirosService.listaVampiros(SETEMBRO)).thenReturn(List.of(recorrencia()));
        when(cartaoService.consultaCartoes(SETEMBRO)).thenReturn(resumoCartoes());
        when(cartaoRepository.buscaAtivos()).thenReturn(List.of(cartaoManual()));
        when(projecaoService.projeta(eq(SETEMBRO), eq(Estrategia.AVALANCHE), eq(60)))
                .thenReturn(projecao());
        when(reservaService.consultaPainel()).thenReturn(painelDeReserva());
        when(planoAjusteService.gera(eq(SETEMBRO), eq(3), eq(new java.math.BigDecimal("0.35"))))
                .thenReturn(planoDeAjuste());

        DashboardResponse dashboard = servico().monta(SETEMBRO);

        assertThat(dashboard.competencia()).isEqualTo("2026-09");
        assertThat(dashboard.viabilidade().veredito()).isEqualTo(Veredito.VIAVEL);

        assertThat(dashboard.euDoPassado().compromissosFuturos()).hasSize(1);
        assertThat(dashboard.euDoPassado().compromissosFuturos().get(0).descricao())
                .isEqualTo("Notebook 6/10");
        assertThat(dashboard.euDoPassado().dividas()).hasSize(1);

        assertThat(dashboard.euDoPresente().diagnostico().competencia()).isEqualTo("2026-09");
        assertThat(dashboard.euDoPresente().resumoTriagem()).hasSize(1);
        assertThat(dashboard.euDoPresente().vampiros()).hasSize(1);
        assertThat(dashboard.euDoPresente().cartoes().totalGastoEmCartoes()).isEqualTo(Dinheiro.de("450.00"));
        assertThat(dashboard.euDoPresente().cartoesManuais()).hasSize(1);
        assertThat(dashboard.euDoPresente().cartoesManuais().get(0).nome()).isEqualTo("PicPay");

        assertThat(dashboard.euDoFuturo().marcos()).isNotNull();
        assertThat(dashboard.euDoFuturo().reserva().custoMensal()).isEqualTo(Dinheiro.de("5320.00"));
        assertThat(dashboard.euDoFuturo().reservaIndisponivel()).isNull();
        assertThat(dashboard.euDoFuturo().planoAjuste().competenciaInicio()).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("mantém o dashboard disponível quando falta o orçamento necessário para a reserva")
    void dashboardSemOrcamentoDaReserva() {
        preparaDashboardBasico();
        when(reservaService.consultaPainel()).thenThrow(APIException.build(HttpStatus.NOT_FOUND,
                "Nenhum orçamento de gastos variáveis definido para 2026-09."));

        DashboardResponse dashboard = servico().monta(SETEMBRO);

        assertThat(dashboard.euDoFuturo().reserva()).isNull();
        assertThat(dashboard.euDoFuturo().reservaIndisponivel())
                .isEqualTo("Nenhum orçamento de gastos variáveis definido para 2026-09.");
        assertThat(dashboard.euDoPresente()).isNotNull();
        assertThat(dashboard.euDoFuturo().marcos()).isNotNull();
        assertThat(dashboard.euDoFuturo().planoAjuste()).isNotNull();
    }

    @Test
    @DisplayName("busca compromissos futuros num horizonte de 12 meses a partir da competência pedida")
    void horizonteDeDozeMeses() {
        when(viabilidadeService.consultaViabilidade(SETEMBRO)).thenReturn(viabilidade());
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO.mais(11)))
                .thenReturn(List.of());
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO)).thenReturn(List.of());
        when(diagnosticoService.consultaSaldoDeSobrevivencia(SETEMBRO)).thenReturn(saldo());
        when(triagemService.resumo(SETEMBRO)).thenReturn(List.of());
        when(vampirosService.listaVampiros(SETEMBRO)).thenReturn(List.of());
        when(cartaoService.consultaCartoes(SETEMBRO)).thenReturn(resumoCartoes());
        when(cartaoRepository.buscaAtivos()).thenReturn(List.of());
        when(projecaoService.projeta(eq(SETEMBRO), eq(Estrategia.AVALANCHE), eq(60)))
                .thenReturn(projecao());
        when(reservaService.consultaPainel()).thenReturn(painelDeReserva());
        when(planoAjusteService.gera(eq(SETEMBRO), eq(3), eq(new java.math.BigDecimal("0.35"))))
                .thenReturn(planoDeAjuste());

        servico().monta(SETEMBRO);

        org.mockito.Mockito.verify(compromissoFuturoRepository)
                .buscaPorPeriodo(SETEMBRO, Competencia.de(2027, 8));
    }

    private DashboardApplicationService servico() {
        return new DashboardApplicationService(viabilidadeService, diagnosticoService, triagemService,
                vampirosService, reservaService, planoAjusteService, projecaoService,
                compromissoFuturoRepository, catalogoRepository, cartaoService, cartaoRepository);
    }

    private void preparaDashboardBasico() {
        when(viabilidadeService.consultaViabilidade(SETEMBRO)).thenReturn(viabilidade());
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO.mais(11))).thenReturn(List.of());
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO)).thenReturn(List.of());
        when(diagnosticoService.consultaSaldoDeSobrevivencia(SETEMBRO)).thenReturn(saldo());
        when(triagemService.resumo(SETEMBRO)).thenReturn(List.of());
        when(vampirosService.listaVampiros(SETEMBRO)).thenReturn(List.of());
        when(cartaoService.consultaCartoes(SETEMBRO)).thenReturn(resumoCartoes());
        when(cartaoRepository.buscaAtivos()).thenReturn(List.of());
        when(projecaoService.projeta(eq(SETEMBRO), eq(Estrategia.AVALANCHE), eq(60))).thenReturn(projecao());
        when(planoAjusteService.gera(eq(SETEMBRO), eq(3), eq(new java.math.BigDecimal("0.35"))))
                .thenReturn(planoDeAjuste());
    }

    private Viabilidade viabilidade() {
        return new Viabilidade(SETEMBRO, Dinheiro.de("10000.00"), Dinheiro.de("4264.05"),
                Dinheiro.de("1310.00"), Dinheiro.de("5574.05"), Dinheiro.de("4425.95"),
                Percentual.deFracao(new java.math.BigDecimal("0.4426")),
                Percentual.deFracao(new java.math.BigDecimal("0.25")), Veredito.VIAVEL,
                Dinheiro.ZERO, null, "dá para bater a meta sem mexer no padrão de vida");
    }

    private CompromissoFuturo compromisso() {
        return new CompromissoFuturo(UUID.randomUUID(), "conta-1", "Notebook 6/10",
                "notebook", null, SETEMBRO, Dinheiro.de("350.00"), 6, 10, true);
    }

    private Divida divida() {
        return new Divida(UUID.randomUUID(), "Empréstimo Nubank", Dinheiro.de("2058.05"),
                Competencia.de(2026, 9), null);
    }

    private SaldoDeSobrevivencia saldo() {
        return new SaldoDeSobrevivencia(SETEMBRO, Dinheiro.de("10000.00"), Dinheiro.de("4264.05"),
                Dinheiro.de("1310.00"), Dinheiro.de("2058.05"), Dinheiro.de("7632.10"),
                Dinheiro.de("2367.90"), false, Dinheiro.ZERO);
    }

    private Recorrencia recorrencia() {
        return new Recorrencia("netflix", Periodicidade.MENSAL, Dinheiro.de("42.40"),
                Dinheiro.de("508.80"), Percentual.deFracao(new java.math.BigDecimal("0.9")),
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 8, 10), 8, false, true);
    }

    private Projecao projecao() {
        MesProjetado mes = new MesProjetado(SETEMBRO, Dinheiro.de("10000.00"), Dinheiro.de("5320.00"),
                Dinheiro.de("700.00"), Dinheiro.de("3980.00"), Dinheiro.ZERO, Dinheiro.ZERO,
                Dinheiro.de("1000.00"), Dinheiro.ZERO, false);
        return new Projecao(SETEMBRO, Estrategia.AVALANCHE, List.of(mes),
                new Marcos(null, null, null, Dinheiro.ZERO, null), StatusProjecao.VIAVEL, null);
    }

    private PainelDeReserva painelDeReserva() {
        return new PainelDeReserva(Dinheiro.de("5320.00"),
                List.of(new StatusDoNivel(NivelDeReserva.UM_MES, Dinheiro.de("5320.00"), false, null)),
                NivelDeReserva.UM_MES);
    }

    private PlanoDeAjuste planoDeAjuste() {
        return new PlanoDeAjuste(SETEMBRO, List.of(), List.of(), List.of(), Dinheiro.ZERO);
    }

    private ResumoCartoesResponse resumoCartoes() {
        CartaoResponse cartao = new CartaoResponse("conta-nubank-cartao", "Nubank",
                Dinheiro.de("3000.00"), Dinheiro.de("450.00"),
                Percentual.deValor(Dinheiro.de("450.00"), Dinheiro.de("3000.00")));
        return ResumoCartoesResponse.de(List.of(cartao));
    }

    private Cartao cartaoManual() {
        return new Cartao(UUID.randomUUID(), "PicPay", null, Dinheiro.de("620.00"),
                null, true);
    }
}

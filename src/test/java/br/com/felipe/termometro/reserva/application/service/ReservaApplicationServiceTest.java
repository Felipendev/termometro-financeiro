package br.com.felipe.termometro.reserva.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.orcamento.application.repository.OrcamentoRepository;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Marcos;
import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.projecao.domain.StatusProjecao;
import br.com.felipe.termometro.reserva.domain.NivelDeReserva;
import br.com.felipe.termometro.reserva.domain.PainelDeReserva;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaApplicationService")
class ReservaApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    private static final Clock RELOGIO_EM_SETEMBRO = Clock.fixed(
            LocalDate.of(2026, 9, 10).atStartOfDay(Competencia.FUSO).toInstant(), Competencia.FUSO);

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private ProjecaoService projecaoService;

    @Mock
    private TotaisMarcadosDoMes totaisMarcados;

    @BeforeEach
    void mantemCatalogoComoFallback() {
        lenient().when(totaisMarcados.marcadoOuLegado(any(), any(), any())).thenAnswer(chamada -> chamada.getArgument(2));
    }

    @Test
    @DisplayName("compõe custo fixo + verba variável e delega os cruzamentos para a simulação")
    void componhaCustoMensalEDelegue() {
        when(catalogoRepository.buscaCustoFixoAtivo())
                .thenReturn(List.of(item("Aluguel", "2200.00"), item("Internet", "120.00")));
        when(orcamentoRepository.buscaVerbaPorCompetencia(SETEMBRO))
                .thenReturn(Optional.of(new VerbaMensal(SETEMBRO, Dinheiro.de("3000.00"), Dinheiro.de("250.00"))));
        when(projecaoService.projeta(eq(SETEMBRO), eq(Estrategia.AVALANCHE), eq(60)))
                .thenReturn(projecaoDe(List.of(
                        mes(SETEMBRO, "1000.00"),
                        mes(SETEMBRO.mais(1), "6000.00"))));

        PainelDeReserva painel = new ReservaApplicationService(catalogoRepository, orcamentoRepository,
                projecaoService, RELOGIO_EM_SETEMBRO, totaisMarcados).consultaPainel();

        // custo mensal = 2200 + 120 (fixo) + 3000 (verba variável, sem provisão à parte) = 5320
        assertThat(painel.custoMensal()).isEqualTo(Dinheiro.de("5320.00"));
        assertThat(painel.niveis()).hasSize(3);
        assertThat(painel.proximoNivel()).isEqualTo(NivelDeReserva.UM_MES);
    }

    @Test
    @DisplayName("sem verba variável declarada para o mês corrente, 404 explícito")
    void semVerbaDeclaradaEhErro() {
        when(catalogoRepository.buscaCustoFixoAtivo()).thenReturn(List.of(item("Aluguel", "2200.00")));
        when(orcamentoRepository.buscaVerbaPorCompetencia(SETEMBRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ReservaApplicationService(catalogoRepository, orcamentoRepository,
                projecaoService, RELOGIO_EM_SETEMBRO, totaisMarcados).consultaPainel())
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Nenhum orçamento de gastos variáveis definido");
    }

    private CustoFixoItem item(String nome, String valor) {
        return new CustoFixoItem(UUID.randomUUID(), nome, Dinheiro.de(valor), "CARTAO", null, true);
    }

    private MesProjetado mes(Competencia competencia, String reservaAcumulada) {
        return new MesProjetado(competencia, Dinheiro.de("10000.00"), Dinheiro.de("5320.00"),
                Dinheiro.de("700.00"), Dinheiro.de("3980.00"), Dinheiro.ZERO, Dinheiro.ZERO,
                Dinheiro.de(reservaAcumulada), Dinheiro.ZERO, false);
    }

    private Projecao projecaoDe(List<MesProjetado> meses) {
        return new Projecao(SETEMBRO, Estrategia.AVALANCHE, meses,
                new Marcos(null, null, null, Dinheiro.ZERO, null), StatusProjecao.VIAVEL, null);
    }
}

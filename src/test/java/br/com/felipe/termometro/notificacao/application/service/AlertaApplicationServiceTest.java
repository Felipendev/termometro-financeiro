package br.com.felipe.termometro.notificacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.notificacao.application.repository.EstadoDeAlertaRepository;
import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import br.com.felipe.termometro.orcamento.application.service.OrcamentoService;
import br.com.felipe.termometro.orcamento.domain.AcaoPossivel;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Marcos;
import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.projecao.domain.StatusProjecao;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertaApplicationService")
class AlertaApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final Clock RELOGIO = Clock.fixed(
            LocalDate.of(2026, 9, 10).atStartOfDay(Competencia.FUSO).toInstant(), Competencia.FUSO);

    @Mock
    private CanalDeNotificacao canal;

    @Mock
    private EstadoDeAlertaRepository estadoRepository;

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private ProjecaoService projecaoService;

    private AlertaApplicationService servico() {
        return new AlertaApplicationService(canal, estadoRepository, orcamentoService, projecaoService, RELOGIO);
    }

    @Nested
    @DisplayName("transações altas")
    class TransacoesAltas {

        @Test
        @DisplayName("canal desabilitado: nem filtra, nada é enviado")
        void canalDesabilitado() {
            when(canal.habilitado()).thenReturn(false);

            servico().avaliaTransacoesAltas(List.of(despesa("-150.00")));

            verify(canal, never()).envia(anyString());
        }

        @Test
        @DisplayName("transação acima do limite dispara uma mensagem consolidada")
        void disparaParaTransacaoAlta() {
            when(canal.habilitado()).thenReturn(true);

            servico().avaliaTransacoesAltas(List.of(despesa("-150.00"), despesa("-30.00")));

            verify(canal).envia(org.mockito.ArgumentMatchers.contains("150,00"));
        }

        @Test
        @DisplayName("nenhuma transação acima do limite: nada é enviado")
        void naoDisparaSemTransacaoAlta() {
            when(canal.habilitado()).thenReturn(true);

            servico().avaliaTransacoesAltas(List.of(despesa("-30.00")));

            verify(canal, never()).envia(anyString());
        }
    }

    @Nested
    @DisplayName("verba baixa")
    class VerbaBaixa {

        @Test
        @DisplayName("canal desabilitado: nem consulta a verba")
        void canalDesabilitado() {
            when(canal.habilitado()).thenReturn(false);

            servico().avaliaVerbaBaixa();

            verify(orcamentoService, never()).consultaVerbaDeHoje();
        }

        @Test
        @DisplayName("primeira vez em RUIM no dia: dispara e grava o estado")
        void primeiraVezDispara() {
            when(canal.habilitado()).thenReturn(true);
            when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaCom(FaixaSaude.RUIM, List.of()));
            when(estadoRepository.busca("verba-baixa:2026-09-10")).thenReturn(Optional.empty());

            servico().avaliaVerbaBaixa();

            verify(canal).envia(anyString());
            verify(estadoRepository).salva("verba-baixa:2026-09-10", "RUIM");
        }

        @Test
        @DisplayName("já avisado RUIM hoje: não dispara de novo")
        void naoRepeteMesmaFaixa() {
            when(canal.habilitado()).thenReturn(true);
            when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaCom(FaixaSaude.RUIM, List.of()));
            when(estadoRepository.busca("verba-baixa:2026-09-10")).thenReturn(Optional.of("RUIM"));

            servico().avaliaVerbaBaixa();

            verify(canal, never()).envia(anyString());
            verify(estadoRepository, never()).salva(anyString(), anyString());
        }

        @Test
        @DisplayName("faixa IDEAL: não dispara, não grava estado")
        void faixaBoaNaoDispara() {
            when(canal.habilitado()).thenReturn(true);
            when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaCom(FaixaSaude.IDEAL, List.of()));

            servico().avaliaVerbaBaixa();

            verify(canal, never()).envia(anyString());
            verify(estadoRepository, never()).salva(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("marco atingido")
    class MarcoAtingido {

        @Test
        @DisplayName("marco de quitação bate com o mês corrente e ainda não foi avisado: dispara")
        void disparaParaMarcoNovo() {
            when(canal.habilitado()).thenReturn(true);
            when(projecaoService.projeta(SETEMBRO, Estrategia.AVALANCHE, 60))
                    .thenReturn(projecaoComMarcos(new Marcos(SETEMBRO, null, null, Dinheiro.ZERO, 1)));
            when(estadoRepository.busca(anyString())).thenReturn(Optional.empty());

            servico().avaliaMarcos();

            verify(canal).envia(org.mockito.ArgumentMatchers.contains("quitada"));
            verify(estadoRepository).salva("marco:quitacao:2026-09", "1");
        }

        @Test
        @DisplayName("marco já avisado nesta competência: não dispara de novo")
        void naoRepeteMarcoJaAvisado() {
            when(canal.habilitado()).thenReturn(true);
            when(projecaoService.projeta(SETEMBRO, Estrategia.AVALANCHE, 60))
                    .thenReturn(projecaoComMarcos(new Marcos(SETEMBRO, null, null, Dinheiro.ZERO, 1)));
            when(estadoRepository.busca("marco:quitacao:2026-09")).thenReturn(Optional.of("1"));

            servico().avaliaMarcos();

            verify(canal, never()).envia(anyString());
        }

        @Test
        @DisplayName("nenhum marco bate com o mês corrente: nada dispara")
        void semMarcoNoMesNadaDispara() {
            when(canal.habilitado()).thenReturn(true);
            when(projecaoService.projeta(SETEMBRO, Estrategia.AVALANCHE, 60))
                    .thenReturn(projecaoComMarcos(new Marcos(SETEMBRO.mais(3), null, null, Dinheiro.ZERO, 4)));

            servico().avaliaMarcos();

            verify(canal, never()).envia(anyString());
        }
    }

    @Nested
    @DisplayName("evento próximo")
    class EventoProximo {

        @Test
        @DisplayName("evento novo na janela de 3 dias: dispara e grava o estado")
        void disparaParaEventoNovo() {
            Evento evento = Evento.previsto(LocalDate.of(2026, 9, 13), "Aniversário", Dinheiro.de("170"));
            when(canal.habilitado()).thenReturn(true);
            when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaCom(FaixaSaude.IDEAL, List.of(evento)));
            when(estadoRepository.busca("evento:2026-09-13:Aniversário")).thenReturn(Optional.empty());

            servico().avaliaEventosProximos();

            verify(canal).envia(org.mockito.ArgumentMatchers.contains("Aniversário"));
            verify(estadoRepository).salva("evento:2026-09-13:Aniversário", "1");
        }

        @Test
        @DisplayName("evento já avisado: não dispara de novo")
        void naoRepeteEventoJaAvisado() {
            Evento evento = Evento.previsto(LocalDate.of(2026, 9, 13), "Aniversário", Dinheiro.de("170"));
            when(canal.habilitado()).thenReturn(true);
            when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaCom(FaixaSaude.IDEAL, List.of(evento)));
            when(estadoRepository.busca("evento:2026-09-13:Aniversário")).thenReturn(Optional.of("1"));

            servico().avaliaEventosProximos();

            verify(canal, never()).envia(anyString());
        }
    }

    private static TransacaoBruta despesa(String valor) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 10), null, "Compra", "Compra", Dinheiro.de(valor),
                null, null, SecaoFatura.CARTAO, null, Origem.CSV, 0);
    }

    private static VerbaDoDia verbaCom(FaixaSaude faixa, List<Evento> eventosProximos) {
        return new VerbaDoDia(LocalDate.of(2026, 9, 10), Dinheiro.de("50.00"), Dinheiro.de("90.00"),
                Dinheiro.de("500.00"), Dinheiro.de("300.00"), Dinheiro.ZERO, 10, faixa, BigDecimal.ONE,
                false, List.<AcaoPossivel>of(), eventosProximos, "mensagem de teste");
    }

    private static Projecao projecaoComMarcos(Marcos marcos) {
        MesProjetado mes = new MesProjetado(SETEMBRO, Dinheiro.de("10000.00"), Dinheiro.de("4000.00"),
                Dinheiro.de("700.00"), Dinheiro.de("5300.00"), Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO,
                Dinheiro.ZERO, false);
        return new Projecao(SETEMBRO, Estrategia.AVALANCHE, List.of(mes), marcos, StatusProjecao.VIAVEL, null);
    }
}

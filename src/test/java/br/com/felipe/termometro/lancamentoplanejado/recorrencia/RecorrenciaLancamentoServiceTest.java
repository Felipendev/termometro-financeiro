package br.com.felipe.termometro.lancamentoplanejado.recorrencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecorrenciaLancamentoServiceTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private final List<LancamentoPlanejado> banco = new ArrayList<>();
    private LancamentoPlanejadoRepository repository;
    private RecorrenciaLancamentoService service;

    @BeforeEach
    void preparaRepositorioEmMemoria() {
        repository = mock(LancamentoPlanejadoRepository.class);
        when(repository.salva(any())).thenAnswer(invocacao -> {
            LancamentoPlanejado item = invocacao.getArgument(0);
            banco.removeIf(existente -> existente.id().equals(item.id()));
            banco.add(item);
            return item;
        });
        when(repository.buscaPorSerie(any())).thenAnswer(invocacao -> {
            UUID serieId = invocacao.getArgument(0);
            return banco.stream().filter(item -> serieId.equals(item.serieId())).toList();
        });
        when(repository.buscaOrfaosDeRecorrencia()).thenAnswer(invocacao -> banco.stream()
                .filter(item -> item.diaRecorrencia() != null && item.serieId() == null)
                .toList());
        service = new RecorrenciaLancamentoService(repository, relogio);
    }

    private LancamentoPlanejado aluguel(LocalDate vencimento) {
        return new LancamentoPlanejado(UUID.randomUUID(), "Aluguel", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("2200"), vencimento, StatusLancamentoPlanejado.PENDENTE, null, null,
                new CategoriaDoLancamento("Casa", "MORADIA", "FIXO"), null, null);
    }

    @Test
    void criaSerieMaterializaDozeMesesComDiaFixo() {
        LancamentoPlanejado origem = aluguel(LocalDate.of(2026, 9, 25));

        LancamentoPlanejado primeira = service.criaSerie(origem, 25);

        assertThat(primeira.serieId()).isNotNull();
        assertThat(banco).hasSize(12);
        assertThat(banco).allMatch(item -> item.vencimento().getDayOfMonth() == 25);
        assertThat(banco).extracting(item -> YearMonth.from(item.vencimento())).containsExactlyInAnyOrder(
                YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11),
                YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2),
                YearMonth.of(2027, 3), YearMonth.of(2027, 4), YearMonth.of(2027, 5),
                YearMonth.of(2027, 6), YearMonth.of(2027, 7), YearMonth.of(2027, 8));
    }

    @Test
    void diaTrintaEUmClampaProUltimoDiaDeFevereiro() {
        LancamentoPlanejado origem = aluguel(LocalDate.of(2026, 9, 30));

        service.criaSerie(origem, 31);

        LancamentoPlanejado fevereiro = banco.stream()
                .filter(item -> YearMonth.from(item.vencimento()).equals(YearMonth.of(2027, 2)))
                .findFirst().orElseThrow();
        assertThat(fevereiro.vencimento()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void adotaLancamentoMarcadoComoRecorrenteQueFicouSemSerie() {
        // estado que o bug deixou no banco: dia fixo gravado, série nunca criada, zero meses futuros
        LancamentoPlanejado orfao = new LancamentoPlanejado(UUID.randomUUID(), "Aluguel",
                TipoLancamentoPlanejado.DESPESA, Dinheiro.de("2200"), LocalDate.of(2026, 9, 10),
                StatusLancamentoPlanejado.LIQUIDADO, null, null,
                new CategoriaDoLancamento("Casa", "MORADIA", "FIXO"), null, null,
                br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento.CUSTO_FIXO,
                null, null, 10);
        repository.salva(orfao);

        int adotados = service.adotaOrfaos();

        assertThat(adotados).isEqualTo(1);
        assertThat(banco).hasSize(12);
        assertThat(banco).allMatch(item -> item.serieId() != null);
        assertThat(banco).allMatch(item -> item.vencimento().getDayOfMonth() == 10);
        assertThat(service.adotaOrfaos()).isZero();
    }

    @Test
    void materializaAteHorizonteEIdempotente() {
        LancamentoPlanejado origem = aluguel(LocalDate.of(2026, 9, 25));
        LancamentoPlanejado primeira = service.criaSerie(origem, 25);

        service.materializaAteHorizonte(primeira.serieId());

        assertThat(banco).hasSize(12);
    }

    @Test
    void materializaAteHorizonteNaoSobrescreveOcorrenciaJaCustomizada() {
        LancamentoPlanejado origem = aluguel(LocalDate.of(2026, 9, 25));
        LancamentoPlanejado primeira = service.criaSerie(origem, 25);
        LancamentoPlanejado novembro = banco.stream()
                .filter(item -> YearMonth.from(item.vencimento()).equals(YearMonth.of(2026, 11)))
                .findFirst().orElseThrow();
        LancamentoPlanejado customizado = new LancamentoPlanejado(novembro.id(), "Aluguel (reajustado)",
                TipoLancamentoPlanejado.DESPESA, Dinheiro.de("2350"), novembro.vencimento(),
                StatusLancamentoPlanejado.PENDENTE, null, null, novembro.categoria(), null, null,
                novembro.marcacaoPlanejamento(), novembro.origemReceita(), novembro.serieId(), 25);
        repository.salva(customizado);

        service.materializaAteHorizonte(primeira.serieId());

        assertThat(banco).hasSize(12);
        LancamentoPlanejado aindaCustomizado = banco.stream()
                .filter(item -> item.id().equals(novembro.id())).findFirst().orElseThrow();
        assertThat(aindaCustomizado.descricao()).isEqualTo("Aluguel (reajustado)");
        assertThat(aindaCustomizado.valor()).isEqualTo(Dinheiro.de("2350"));
    }

    @Test
    void aplicaEdicaoATodasAsFuturasSoMudaPendentesComVencimentoIgualOuPosterior() {
        LancamentoPlanejado origem = aluguel(LocalDate.of(2026, 9, 25));
        LancamentoPlanejado primeira = service.criaSerie(origem, 25);
        LancamentoPlanejado setembro = banco.stream()
                .filter(item -> YearMonth.from(item.vencimento()).equals(YearMonth.of(2026, 9)))
                .findFirst().orElseThrow();
        LancamentoPlanejado outubro = banco.stream()
                .filter(item -> YearMonth.from(item.vencimento()).equals(YearMonth.of(2026, 10)))
                .findFirst().orElseThrow();
        // outubro já foi liquidado antes da edição — não deve ser tocado.
        repository.salva(outubro.liquidar());
        LancamentoPlanejado editadoDeSetembroEmDiante = new LancamentoPlanejado(setembro.id(),
                "Aluguel (reajustado)", TipoLancamentoPlanejado.DESPESA, Dinheiro.de("2500"),
                setembro.vencimento(), StatusLancamentoPlanejado.PENDENTE, null, null,
                setembro.categoria(), null, null, setembro.marcacaoPlanejamento(),
                setembro.origemReceita(), setembro.serieId(), 28);

        service.aplicaEdicaoATodasAsFuturas(editadoDeSetembroEmDiante);

        LancamentoPlanejado outubroDepois = banco.stream()
                .filter(item -> item.id().equals(outubro.id())).findFirst().orElseThrow();
        assertThat(outubroDepois.descricao()).isEqualTo("Aluguel");
        assertThat(outubroDepois.status()).isEqualTo(StatusLancamentoPlanejado.LIQUIDADO);

        LancamentoPlanejado novembroDepois = banco.stream()
                .filter(item -> YearMonth.from(item.vencimento()).equals(YearMonth.of(2026, 11)))
                .findFirst().orElseThrow();
        assertThat(novembroDepois.descricao()).isEqualTo("Aluguel (reajustado)");
        assertThat(novembroDepois.valor()).isEqualTo(Dinheiro.de("2500"));
        assertThat(novembroDepois.vencimento().getDayOfMonth()).isEqualTo(28);
    }
}

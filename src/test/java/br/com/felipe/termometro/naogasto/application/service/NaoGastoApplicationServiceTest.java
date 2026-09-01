package br.com.felipe.termometro.naogasto.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.naogasto.application.repository.NaoGastoRepository;
import br.com.felipe.termometro.naogasto.domain.LancamentoParaConciliar;
import br.com.felipe.termometro.naogasto.domain.ResultadoDaConciliacao;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NaoGastoApplicationServiceTest {

    private static final Competencia COMPETENCIA = Competencia.de(2026, 8);

    @Mock
    private NaoGastoRepository naoGastoRepository;

    private NaoGastoApplicationService service;

    @Test
    @DisplayName("consulta desde 2 meses antes da competência até o fim dela")
    void consultaOJanelaDeContexto() {
        service = new NaoGastoApplicationService(naoGastoRepository);
        when(naoGastoRepository.buscaLancamentos(any(), any())).thenReturn(List.of());

        service.concilia(COMPETENCIA);

        verify(naoGastoRepository).buscaLancamentos(
                eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 8, 31)));
    }

    @Test
    @DisplayName("casamentos encontrados são persistidos via marcaIgnoradas")
    void casamentosSaoPersistidos() {
        service = new NaoGastoApplicationService(naoGastoRepository);
        UUID compra = UUID.randomUUID();
        UUID debito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                new LancamentoParaConciliar(compra, "itau-cartao", LocalDate.of(2026, 7, 5),
                        Dinheiro.de("-1000.00"), "compra", null, SecaoFatura.CARTAO),
                new LancamentoParaConciliar(debito, "itau-corrente", LocalDate.of(2026, 8, 5),
                        Dinheiro.de("-1000.00"), "pagamento", null, SecaoFatura.MOVIMENTO_CONTA));
        when(naoGastoRepository.buscaLancamentos(any(), any())).thenReturn(lancamentos);

        ResultadoDaConciliacao resultado = service.concilia(COMPETENCIA);

        assertThat(resultado.pagamentosDeFaturaCasados()).isEqualTo(1);
        ArgumentCaptor<Set<UUID>> captor = ArgumentCaptor.forClass(Set.class);
        verify(naoGastoRepository).marcaIgnoradas(captor.capture());
        assertThat(captor.getValue()).containsExactly(debito);
    }

    @Test
    @DisplayName("sem casamentos, marcaIgnoradas nunca é chamado")
    void semCasamentosNaoChamaMarcaIgnoradas() {
        service = new NaoGastoApplicationService(naoGastoRepository);
        when(naoGastoRepository.buscaLancamentos(any(), any())).thenReturn(List.of(
                new LancamentoParaConciliar(UUID.randomUUID(), "itau-cartao", LocalDate.of(2026, 8, 3),
                        Dinheiro.de("-45.00"), "compra", null, SecaoFatura.CARTAO)));

        service.concilia(COMPETENCIA);

        verify(naoGastoRepository, never()).marcaIgnoradas(any());
    }
}

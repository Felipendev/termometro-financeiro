package br.com.felipe.termometro.compromissofuturo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.LancamentoParceladoAncora;
import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompromissoFuturoApplicationService")
class CompromissoFuturoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private CompromissoFuturoRepository repository;

    @Test
    @DisplayName("busca as parceladas, gera os compromissos e reconcilia")
    void geraEReconciliaComOResultadoDoMotor() {
        LancamentoParceladoAncora ancora = new LancamentoParceladoAncora("CONTA-1", "LOJA X",
                "LOJA X - Parcela 3/10", "COMPRAS", SETEMBRO, Dinheiro.de("-150.00"), 3, 10);
        when(repository.buscaTodosLancamentosParcelados()).thenReturn(List.of(ancora));

        ResultadoDaGeracao resultado = new CompromissoFuturoApplicationService(repository).gera();

        assertThat(resultado.gerados()).hasSize(7);
        assertThat(resultado.seriesProcessadas()).hasSize(1);
        verify(repository).reconcilia(resultado);
    }

    @Test
    @DisplayName("sem lançamentos parcelados, reconcilia com resultado vazio (nunca pula a reconciliação)")
    void semLancamentosAindaAssimReconciliaParaLimparSeriesEncerradas() {
        when(repository.buscaTodosLancamentosParcelados()).thenReturn(List.of());

        CompromissoFuturoApplicationService service = new CompromissoFuturoApplicationService(repository);
        ResultadoDaGeracao resultado = service.gera();

        assertThat(resultado.gerados()).isEmpty();
        assertThat(resultado.seriesProcessadas()).isEmpty();
        verify(repository).reconcilia(resultado);
        verify(repository, never()).buscaPorPeriodo(any(), any());
    }
}

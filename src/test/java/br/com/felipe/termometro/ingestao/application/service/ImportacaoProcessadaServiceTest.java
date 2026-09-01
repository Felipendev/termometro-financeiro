package br.com.felipe.termometro.ingestao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.classificacao.application.api.response.ResultadoDaClassificacaoResponse;
import br.com.felipe.termometro.classificacao.application.service.ClassificacaoService;
import br.com.felipe.termometro.compromissofuturo.application.service.CompromissoFuturoService;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.naogasto.application.service.NaoGastoService;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.service.TriagemService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportacaoProcessadaServiceTest {

    @Mock private ClassificacaoService classificacaoService;
    @Mock private NaoGastoService naoGastoService;
    @Mock private CompromissoFuturoService compromissoFuturoService;
    @Mock private TriagemService triagemService;

    @Test
    void processaCadaCompetenciaNovaUmaVezNaOrdemDoPipeline() {
        Competencia julho = Competencia.parse("2026-07");
        Competencia agosto = Competencia.parse("2026-08");
        when(classificacaoService.classifica(julho)).thenReturn(classificacao(julho, 2, 1));
        when(classificacaoService.classifica(agosto)).thenReturn(classificacao(agosto, 3, 2));
        when(triagemService.executaTriagem(julho)).thenReturn(triagem(julho, 2));
        when(triagemService.executaTriagem(agosto)).thenReturn(triagem(agosto, 3));

        ImportacaoProcessadaService service = new ImportacaoProcessadaService(
                classificacaoService, naoGastoService, compromissoFuturoService, triagemService);

        ResultadoDoProcessamentoImportacao resultado = service.processa(List.of(
                transacao("2026-08-03"), transacao("2026-07-15"), transacao("2026-08-04")));

        assertThat(resultado.competenciasProcessadas()).containsExactly(julho, agosto);
        assertThat(resultado.classificadas()).isEqualTo(5);
        assertThat(resultado.pendentesDeRevisao()).isEqualTo(3);
        assertThat(resultado.triadas()).isEqualTo(5);
        InOrder ordem = inOrder(classificacaoService, naoGastoService, compromissoFuturoService, triagemService);
        ordem.verify(classificacaoService).classifica(julho);
        ordem.verify(classificacaoService).classifica(agosto);
        ordem.verify(naoGastoService).concilia(julho);
        ordem.verify(naoGastoService).concilia(agosto);
        ordem.verify(compromissoFuturoService).gera();
        ordem.verify(triagemService).executaTriagem(julho);
        ordem.verify(triagemService).executaTriagem(agosto);
    }

    @Test
    void naoRodaPipelineQuandoAImportacaoNaoTemTransacaoNova() {
        ImportacaoProcessadaService service = new ImportacaoProcessadaService(
                classificacaoService, naoGastoService, compromissoFuturoService, triagemService);

        ResultadoDoProcessamentoImportacao resultado = service.processa(List.of());

        assertThat(resultado.competenciasProcessadas()).isEmpty();
        verify(classificacaoService, never()).classifica(org.mockito.ArgumentMatchers.any());
        verify(naoGastoService, never()).concilia(org.mockito.ArgumentMatchers.any());
        verify(compromissoFuturoService, never()).gera();
        verify(triagemService, never()).executaTriagem(org.mockito.ArgumentMatchers.any());
    }

    private static ResultadoDaClassificacaoResponse classificacao(Competencia competencia, int classificadas,
                                                                   int pendentes) {
        return new ResultadoDaClassificacaoResponse(competencia.toString(), classificadas, classificadas,
                0, pendentes, Map.of(), List.of());
    }

    private static ResultadoDaTriagemResponse triagem(Competencia competencia, int triadas) {
        return new ResultadoDaTriagemResponse(competencia.toString(), triadas, triadas, Map.of());
    }

    private static TransacaoBruta transacao(String data) {
        return new TransacaoBruta(LocalDate.parse(data), null, "Mercado", "Mercado", Dinheiro.de("10").negado(),
                null, null, SecaoFatura.CARTAO, null, Origem.CSV, 0);
    }
}

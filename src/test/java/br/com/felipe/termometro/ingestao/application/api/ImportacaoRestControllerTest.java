package br.com.felipe.termometro.ingestao.application.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.application.service.IngestaoService;
import br.com.felipe.termometro.ingestao.application.service.ImportacaoConcluida;
import br.com.felipe.termometro.ingestao.application.service.ImportacaoProcessadaService;
import br.com.felipe.termometro.ingestao.application.service.ResultadoDoProcessamentoImportacao;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.ingestao.infra.nubank.LeitorNubankCsv;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImportacaoRestControllerTest {

    @Mock
    private IngestaoService ingestaoService;
    @Mock
    private ImportacaoProcessadaService processamentoService;

    @Test
    void importaCsvPeloLeitorNubank() {
        when(ingestaoService.importaArquivoComResultado(eq("nubank-manual"), eq(LeitorNubankCsv.FORMATO), any(InputStream.class)))
                .thenReturn(new ImportacaoConcluida(new ResultadoDaLeitura(List.of(), null, List.of("arquivo sem lançamentos")), List.of()));
        when(processamentoService.processa(List.of())).thenReturn(ResultadoDoProcessamentoImportacao.vazio());
        ImportacaoRestController controller = new ImportacaoRestController(ingestaoService, processamentoService);

        var resposta = controller.importaNubankCsv("nubank-manual",
                new MockMultipartFile("arquivo", "fatura.csv", "text/csv", "date,title,amount\n".getBytes()));

        assertThat(resposta.transacoesLidas()).isZero();
        assertThat(resposta.confiavel()).isTrue();
        assertThat(resposta.avisos()).containsExactly("arquivo sem lançamentos");
        assertThat(resposta.competenciasProcessadas()).isEmpty();
        verify(ingestaoService).importaArquivoComResultado(eq("nubank-manual"), eq(LeitorNubankCsv.FORMATO), any(InputStream.class));
        verify(processamentoService).processa(List.of());
    }
}

package br.com.felipe.termometro.ingestao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.application.leitor.LeitorDeFatura;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Dinheiro;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN-27.1 — detecção automática por conteúdo, sem persistir. */
class IngestaoApplicationServicePropoeImportacaoTest {

    private final TransacaoRepository transacaoRepository = mock(TransacaoRepository.class);

    @Test
    void reconheceUmFormatoQuandoAlgumLeitorProduzTransacoes() throws IOException {
        LeitorDeFatura leitorQueFalha = leitorQueLanca("PDF_ITAU");
        LeitorDeFatura leitorQueReconhece = leitorComTransacao("CSV_NUBANK");

        var service = new IngestaoApplicationService(
                List.of(leitorQueFalha, leitorQueReconhece), transacaoRepository);

        PropostaDeImportacao proposta = service.propoeImportacao("conteudo".getBytes());

        assertThat(proposta.reconhecido()).isTrue();
        assertThat(proposta.formatoDetectado()).isEqualTo("CSV_NUBANK");
        assertThat(proposta.leitura().transacoes()).hasSize(1);
    }

    @Test
    void naoPersisteNadaAoPropor() throws IOException {
        LeitorDeFatura leitor = leitorComTransacao("CSV_NUBANK");
        var service = new IngestaoApplicationService(List.of(leitor), transacaoRepository);

        service.propoeImportacao("conteudo".getBytes());

        verifyNoInteractions(transacaoRepository);
    }

    @Test
    void devolveNaoReconhecidoQuandoNenhumLeitorProduzTransacoes() throws IOException {
        LeitorDeFatura leitor = leitorQueLanca("PDF_ITAU");
        var service = new IngestaoApplicationService(List.of(leitor), transacaoRepository);

        PropostaDeImportacao proposta = service.propoeImportacao("conteudo".getBytes());

        assertThat(proposta.reconhecido()).isFalse();
        assertThat(proposta.formatosDisponiveis()).containsExactly("PDF_ITAU");
    }

    private LeitorDeFatura leitorQueLanca(String formato) throws IOException {
        LeitorDeFatura leitor = mock(LeitorDeFatura.class);
        when(leitor.formato()).thenReturn(formato);
        when(leitor.ler(any(InputStream.class))).thenThrow(new IOException("não é este formato"));
        return leitor;
    }

    private LeitorDeFatura leitorComTransacao(String formato) throws IOException {
        LeitorDeFatura leitor = mock(LeitorDeFatura.class);
        when(leitor.formato()).thenReturn(formato);
        TransacaoBruta transacao = new TransacaoBruta(
                LocalDate.of(2026, 9, 1), null, "compra", "compra", Dinheiro.de("-50"),
                null, null, SecaoFatura.CARTAO, null, Origem.PDF, 0);
        when(leitor.ler(any(InputStream.class)))
                .thenReturn(new ResultadoDaLeitura(List.of(transacao), null, List.of()));
        return leitor;
    }
}

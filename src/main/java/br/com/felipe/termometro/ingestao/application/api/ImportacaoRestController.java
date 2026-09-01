package br.com.felipe.termometro.ingestao.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.api.response.PropostaImportacaoResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResultadoDaImportacaoResponse;
import br.com.felipe.termometro.ingestao.application.service.IngestaoService;
import br.com.felipe.termometro.ingestao.application.service.ImportacaoConcluida;
import br.com.felipe.termometro.ingestao.application.service.ImportacaoProcessadaService;
import br.com.felipe.termometro.ingestao.application.service.ResultadoDoProcessamentoImportacao;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.infra.nubank.LeitorNubankCsv;
import br.com.felipe.termometro.ingestao.infra.pdf.LeitorItauPdf;
import br.com.felipe.termometro.ingestao.infra.pdf.LeitorPicPayPdf;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ImportacaoRestController implements ImportacaoAPI {

    private final IngestaoService ingestaoService;
    private final ImportacaoProcessadaService processamentoService;

    @Override
    public PropostaImportacaoResponse propor(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Envie um arquivo.");
        }
        try {
            return PropostaImportacaoResponse.de(ingestaoService.propoeImportacao(arquivo.getBytes()));
        } catch (IOException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Não consegui abrir o arquivo enviado.", e);
        }
    }

    @Override
    public ResultadoDaImportacaoResponse importaNubankCsv(String identificadorConta, MultipartFile arquivo) {
        return importar(identificadorConta, arquivo, LeitorNubankCsv.FORMATO, "CSV do Nubank");
    }

    @Override
    public ResultadoDaImportacaoResponse importaFaturaPdf(String identificadorConta, String formato,
                                                           MultipartFile arquivo) {
        if (!LeitorItauPdf.FORMATO.equals(formato) && !LeitorPicPayPdf.FORMATO.equals(formato)) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Escolha uma fatura Itaú ou PicPay.");
        }
        return importar(identificadorConta, arquivo, formato, "PDF de fatura");
    }

    private ResultadoDaImportacaoResponse importar(String identificadorConta, MultipartFile arquivo,
                                                    String formato, String tipoArquivo) {
        if (arquivo.isEmpty()) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Envie um " + tipoArquivo + ".");
        }
        log.info("[inicia] ImportacaoRestController - importacao manual [{}]", identificadorConta);
        try (InputStream conteudo = arquivo.getInputStream()) {
            ImportacaoConcluida importacao = ingestaoService.importaArquivoComResultado(
                    identificadorConta, formato, conteudo);
            ResultadoDoProcessamentoImportacao processamento = processamentoService.processa(importacao.novasTransacoes());
            ResultadoDaImportacaoResponse resposta = new ResultadoDaImportacaoResponse(importacao.leitura(), processamento);
            log.info("[finaliza] ImportacaoRestController - importacao manual [{} transações]",
                    resposta.transacoesLidas());
            return resposta;
        } catch (IOException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST, "Não consegui abrir o arquivo enviado.", e);
        }
    }
}

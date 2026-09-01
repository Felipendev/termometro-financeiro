package br.com.felipe.termometro.ingestao.application.api;

import br.com.felipe.termometro.ingestao.application.api.response.PropostaImportacaoResponse;
import br.com.felipe.termometro.ingestao.application.api.response.ResultadoDaImportacaoResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

/** Entrada manual de arquivos enquanto o Open Finance não cobre todas as fontes. */
@RequestMapping("/v1/importacoes")
public interface ImportacaoAPI {

    /** RN-27.1 — detecção automática por conteúdo, sem persistir nada. */
    @PostMapping(value = "/propor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    PropostaImportacaoResponse propor(@RequestParam("arquivo") MultipartFile arquivo);

    @PostMapping(value = "/nubank-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaImportacaoResponse importaNubankCsv(
            @RequestParam @NotBlank String identificadorConta,
            @RequestParam("arquivo") MultipartFile arquivo);

    @PostMapping(value = "/fatura-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    ResultadoDaImportacaoResponse importaFaturaPdf(
            @RequestParam @NotBlank String identificadorConta,
            @RequestParam @NotBlank String formato,
            @RequestParam("arquivo") MultipartFile arquivo);
}

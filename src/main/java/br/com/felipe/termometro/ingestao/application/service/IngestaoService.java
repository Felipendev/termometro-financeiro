package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import java.io.InputStream;

/** Porta de entrada da ingestão: recebe um arquivo de fatura e devolve o que foi lido. */
public interface IngestaoService {

    ResultadoDaLeitura importaArquivo(String identificadorDaConta, String formato, InputStream conteudo);

    default ImportacaoConcluida importaArquivoComResultado(String identificadorDaConta, String formato,
                                                            InputStream conteudo) {
        ResultadoDaLeitura leitura = importaArquivo(identificadorDaConta, formato, conteudo);
        return new ImportacaoConcluida(leitura, leitura.transacoes());
    }

    /**
     * RN-27.1 — tenta cada leitor registrado contra o mesmo conteúdo, sem persistir nada. O
     * primeiro que reconhecer o arquivo (produzir ao menos uma transação) vence.
     */
    PropostaDeImportacao propoeImportacao(byte[] conteudo);
}

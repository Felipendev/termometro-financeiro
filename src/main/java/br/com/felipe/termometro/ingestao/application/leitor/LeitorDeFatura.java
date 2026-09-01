package br.com.felipe.termometro.ingestao.application.leitor;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;

import java.io.IOException;
import java.io.InputStream;

/** Porta de entrada de arquivos. Uma implementação por formato de banco. */
public interface LeitorDeFatura {

    /** Identificador estável do formato, usado em log e no relatório de importação. */
    String formato();

    ResultadoDaLeitura ler(InputStream conteudo) throws IOException;
}

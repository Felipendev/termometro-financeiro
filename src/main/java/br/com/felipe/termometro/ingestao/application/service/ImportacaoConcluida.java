package br.com.felipe.termometro.ingestao.application.service;

import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import java.util.List;

/** Leitura original e o subconjunto novo que acabou de entrar no banco. */
public record ImportacaoConcluida(ResultadoDaLeitura leitura, List<TransacaoBruta> novasTransacoes) {
    public ImportacaoConcluida { novasTransacoes = List.copyOf(novasTransacoes); }
}

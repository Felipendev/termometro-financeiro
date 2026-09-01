package br.com.felipe.termometro.ingestao.application.api.response;

import br.com.felipe.termometro.ingestao.domain.Reconciliacao;
import br.com.felipe.termometro.ingestao.domain.ResultadoDaLeitura;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import br.com.felipe.termometro.ingestao.application.service.ResultadoDoProcessamentoImportacao;
import org.jspecify.annotations.Nullable;

/** Retorno legível para a importação manual: total, conferência e avisos sem expor o arquivo. */
public record ResultadoDaImportacaoResponse(
        int transacoesLidas,
        Dinheiro totalDeDespesas,
        boolean confiavel,
        @Nullable Reconciliacao conciliacao,
        List<String> avisos,
        List<String> competenciasProcessadas) {

    public ResultadoDaImportacaoResponse(ResultadoDaLeitura resultado) {
        this(resultado, ResultadoDoProcessamentoImportacao.vazio());
    }

    public ResultadoDaImportacaoResponse(ResultadoDaLeitura resultado,
                                         ResultadoDoProcessamentoImportacao processamento) {
        this(resultado.transacoes().size(), resultado.totalDeDespesas(), resultado.confiavel(),
                resultado.conferencia().orElse(null), resultado.avisos(),
                processamento.competenciasProcessadas().stream().map(Object::toString).toList());
    }
}

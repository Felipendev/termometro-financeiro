package br.com.felipe.termometro.naogasto.application.api.response;

import br.com.felipe.termometro.naogasto.domain.ResultadoDaConciliacao;
import java.math.BigDecimal;
import java.util.List;

public record ResultadoDaConciliacaoResponse(
        int pagamentosDeFaturaCasados, int transferenciasCasadas, int estornosCasados,
        BigDecimal valorTotalIgnorado, List<String> detalhes) {

    public static ResultadoDaConciliacaoResponse de(ResultadoDaConciliacao resultado) {
        return new ResultadoDaConciliacaoResponse(resultado.pagamentosDeFaturaCasados(),
                resultado.transferenciasCasadas(), resultado.estornosCasados(),
                resultado.valorTotalIgnorado().valor(), resultado.detalhes());
    }
}

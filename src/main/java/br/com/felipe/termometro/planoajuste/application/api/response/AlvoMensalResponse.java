package br.com.felipe.termometro.planoajuste.application.api.response;

import br.com.felipe.termometro.planoajuste.domain.AlvoMensal;
import java.math.BigDecimal;

public record AlvoMensalResponse(int mes, BigDecimal alvo, BigDecimal reducaoPercentual) {

    public static AlvoMensalResponse de(AlvoMensal alvo) {
        return new AlvoMensalResponse(alvo.mes(), alvo.alvo().valor(), alvo.reducaoPercentual().fracao());
    }
}

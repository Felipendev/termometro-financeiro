package br.com.felipe.termometro.compromissofuturo.application.api.response;

import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;

public record ResultadoDaGeracaoResponse(int compromissosGerados, int seriesProcessadas) {

    public static ResultadoDaGeracaoResponse de(ResultadoDaGeracao resultado) {
        return new ResultadoDaGeracaoResponse(resultado.gerados().size(), resultado.seriesProcessadas().size());
    }
}

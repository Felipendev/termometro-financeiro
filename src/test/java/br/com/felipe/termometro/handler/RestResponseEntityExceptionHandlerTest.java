package br.com.felipe.termometro.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.mockito.Mockito;

class RestResponseEntityExceptionHandlerTest {

    @Test
    void parametroObrigatorioAusenteRetornaBadRequest() {
        var resposta = new RestResponseEntityExceptionHandler().handleParametroObrigatorio(
                new MissingServletRequestParameterException("competencia", "String"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("competencia");
    }

    @Test
    void jsonDeOutraVersaoRetornaErroDeContratoSemVirarErroInterno() {
        var resposta = new RestResponseEntityExceptionHandler().handleCorpoIncompativel(
                Mockito.mock(HttpMessageNotReadableException.class));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("versão atual do servidor");
    }
}

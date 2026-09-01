package br.com.felipe.termometro.ingestao.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.api.response.ResumoCartoesResponse;
import br.com.felipe.termometro.ingestao.application.service.CartaoService;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CartaoRestController implements CartaoAPI {

    private final CartaoService cartaoService;

    @Override
    public ResumoCartoesResponse getCartoes(String competencia) {
        return cartaoService.consultaCartoes(competenciaDe(competencia));
    }

    private Competencia competenciaDe(String competencia) {
        try {
            return Competencia.parse(competencia);
        } catch (DateTimeParseException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Competência inválida: '" + competencia + "'. Use o formato AAAA-MM.", e);
        }
    }
}

package br.com.felipe.termometro.projecao.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.projecao.application.api.response.ProjecaoResponse;
import br.com.felipe.termometro.projecao.application.service.ProjecaoService;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.shared.Competencia;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ProjecaoRestController implements ProjecaoAPI {

    private final ProjecaoService projecaoService;

    @Override
    public ProjecaoResponse getProjecao(String competencia, String estrategia, int horizonteMeses) {
        log.info("[inicia] ProjecaoRestController - getProjecao");
        ProjecaoResponse resposta = new ProjecaoResponse(projecaoService.projeta(
                competenciaDe(competencia), estrategiaDe(estrategia), horizonteMesesValido(horizonteMeses)));
        log.info("[finaliza] ProjecaoRestController - getProjecao");
        return resposta;
    }

    private Competencia competenciaDe(String competencia) {
        try {
            return Competencia.parse(competencia);
        } catch (DateTimeParseException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Competência inválida: '" + competencia + "'. Use o formato AAAA-MM.", e);
        }
    }

    private Estrategia estrategiaDe(String estrategia) {
        try {
            return Estrategia.valueOf(estrategia.toUpperCase());
        } catch (IllegalArgumentException e) {
            String validas = Arrays.stream(Estrategia.values()).map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "Estratégia inválida: '" + estrategia + "'. Use uma de: " + validas + ".", e);
        }
    }

    private int horizonteMesesValido(int horizonteMeses) {
        if (horizonteMeses <= 0) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "horizonteMeses deve ser positivo, recebido: " + horizonteMeses + ".");
        }
        return horizonteMeses;
    }
}

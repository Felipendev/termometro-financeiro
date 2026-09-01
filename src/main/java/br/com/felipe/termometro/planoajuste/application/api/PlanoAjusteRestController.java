package br.com.felipe.termometro.planoajuste.application.api;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.planoajuste.application.api.response.PlanoDeAjusteResponse;
import br.com.felipe.termometro.planoajuste.application.service.PlanoAjusteService;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PlanoAjusteRestController implements PlanoAjusteAPI {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final PlanoAjusteService planoAjusteService;

    @Override
    public PlanoDeAjusteResponse plano(String competencia, int mesesRampa, int fatorMaxCortePercentual) {
        Competencia referencia = competenciaDe(competencia);
        if (mesesRampa < 1) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "mesesRampa deve ser >= 1: " + mesesRampa);
        }
        if (fatorMaxCortePercentual <= 0 || fatorMaxCortePercentual >= 100) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "fatorMaxCortePercentual deve estar entre 1 e 99: " + fatorMaxCortePercentual);
        }
        BigDecimal fatorMaxCorte = BigDecimal.valueOf(fatorMaxCortePercentual).divide(CEM);

        PlanoDeAjuste plano = planoAjusteService.gera(referencia, mesesRampa, fatorMaxCorte);
        return PlanoDeAjusteResponse.de(plano);
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

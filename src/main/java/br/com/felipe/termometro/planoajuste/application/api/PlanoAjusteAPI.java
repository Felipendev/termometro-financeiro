package br.com.felipe.termometro.planoajuste.application.api;

import br.com.felipe.termometro.planoajuste.application.api.response.PlanoDeAjusteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * RN-15 — Plano de Ajuste Progressivo.
 *
 * <p><b>Nota de escopo:</b> a spec modela {@code plano_ajuste}/{@code plano_ajuste_item} como
 * entidades persistidas, associadas a um {@code cenario_id} que ainda não existe no código (mesma
 * lacuna já documentada em {@code ProjecaoAPI} e {@code TriagemAPI}). Enquanto isso, o plano é
 * calculado sob demanda a cada chamada — mesmo padrão de {@code /viabilidade} e {@code
 * /diagnostico} — e migra para o contrato persistido da spec quando {@code cenario} existir.
 */
@RestController
@RequestMapping("/v1/plano-ajuste")
public interface PlanoAjusteAPI {

    /**
     * @param competencia             a rampa começa a valer a partir desta competência (AAAA-MM);
     *                                os "últimos 3 meses fechados" da RN-15 são lidos antes dela
     * @param mesesRampa              horizonte pedido; pode ser alongado (RN-15) — default 3
     * @param fatorMaxCortePercentual corte máximo mês a mês, em pontos percentuais (ex. 35 = 35%)
     *                                — default 35
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    PlanoDeAjusteResponse plano(@RequestParam String competencia,
            @RequestParam(required = false, defaultValue = "3") int mesesRampa,
            @RequestParam(required = false, defaultValue = "35") int fatorMaxCortePercentual);
}

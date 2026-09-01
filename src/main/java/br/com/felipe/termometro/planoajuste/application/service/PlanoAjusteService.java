package br.com.felipe.termometro.planoajuste.application.service;

import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import java.math.BigDecimal;

/** RN-15 — porta de entrada do plano de ajuste progressivo. */
public interface PlanoAjusteService {

    /**
     * @param referencia            a rampa começa a valer a partir desta competência; os "últimos
     *                              3 meses fechados" da RN-15 são lidos antes dela
     * @param mesesRampaSolicitados horizonte pedido; pode ser alongado (RN-15) — default 3
     * @param fatorMaxCorte         corte máximo mês a mês, fração 0-1 — default 0,35
     */
    PlanoDeAjuste gera(Competencia referencia, int mesesRampaSolicitados, BigDecimal fatorMaxCorte);
}

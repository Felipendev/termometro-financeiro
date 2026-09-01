package br.com.felipe.termometro.planilha.domain;

/**
 * RN-18 — o que uma compra no cartão revela sobre o momento em que aconteceu. Três níveis, não
 * dois: sinalizar sem acusar (ATENCAO) é diferente de "isso é sintoma" (DEFICIT_DISFARCADO).
 */
public enum UsoDeCredito {
    FERRAMENTA,
    ATENCAO,
    DEFICIT_DISFARCADO
}

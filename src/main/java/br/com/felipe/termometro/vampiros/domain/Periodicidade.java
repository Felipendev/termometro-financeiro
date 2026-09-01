package br.com.felipe.termometro.vampiros.domain;

/**
 * RN-07 reconhece recorrência mensal (intervalo mediano entre 26 e 35 dias) e anual (entre 350
 * e 380 dias). {@code SEMANAL} aparece no schema da spec (seção 4) mas a regra de negócio (seção
 * 5) não dá a faixa de dias correspondente — não incluída aqui para não inventar um limiar que a
 * spec não definiu.
 */
public enum Periodicidade {
    MENSAL,
    ANUAL
}

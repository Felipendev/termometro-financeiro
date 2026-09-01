package br.com.felipe.termometro.ingestao.infra;

import java.math.BigDecimal;

/** Projeção Spring Data para a soma agrupada por conta — ver TransacaoSpringDataJpaRepository. */
interface GastoPorContaProjection {

    String getIdentificadorConta();

    BigDecimal getTotal();
}

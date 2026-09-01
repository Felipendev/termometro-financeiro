package br.com.felipe.termometro.orcamento.infra;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha do read model de gastos agregados por dia. */
interface GastoDiarioProjection {

    LocalDate getData();

    BigDecimal getTotal();
}

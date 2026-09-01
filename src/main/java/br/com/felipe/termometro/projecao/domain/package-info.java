/**
 * Projeção (RN-09): "quando eu quito?" — simulação determinística mês a mês da dívida em
 * aberto sendo paga, dado o fluxo de caixa esperado (renda, custo fixo, piso variável) e uma
 * estratégia de amortização. Depende só do que {@code catalogo} e {@code diagnostico} já sabem
 * calcular; não lê transação nenhuma diretamente.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.projecao.domain;

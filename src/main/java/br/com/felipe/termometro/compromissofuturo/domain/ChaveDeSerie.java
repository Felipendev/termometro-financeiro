package br.com.felipe.termometro.compromissofuturo.domain;

/**
 * Identifica uma série de parcelas sem depender de nenhum id de compra persistido — este
 * código nunca teve um {@code compra_origem_id} preenchido (o campo existe no schema da spec,
 * mas nenhum leitor ou adapter grava nele). Conta + estabelecimento normalizado + total de
 * parcelas é a melhor aproximação disponível: duas compras diferentes, no mesmo
 * estabelecimento, na mesma conta, com o mesmo número de parcelas, no mesmo mês, colidiriam —
 * cenário raro o bastante para não justificar inventar um id de correlação sem base real.
 */
public record ChaveDeSerie(String identificadorConta, String descricaoNormalizada, int parcelaTotal) {
}

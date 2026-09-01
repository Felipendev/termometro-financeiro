/**
 * Ingestão: transforma fatura e extrato em {@link br.com.felipe.termometro.ingestao.domain.TransacaoBruta}.
 *
 * <p>Este módulo não classifica e não julga — ele só garante que o que entrou é fiel ao que o
 * banco emitiu. Duas invariantes sustentam tudo o que vem depois:
 *
 * <ol>
 *   <li><b>RN-01</b> — sinal normalizado: saída negativa, entrada positiva, sempre.</li>
 *   <li><b>Reconciliação</b> — a soma do que foi lido tem que bater com o total impresso na
 *       fatura. Um parser que lê 97% dos lançamentos e não avisa produz um diagnóstico
 *       silenciosamente errado, que é pior que nenhum diagnóstico.</li>
 * </ol>
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.ingestao.domain;

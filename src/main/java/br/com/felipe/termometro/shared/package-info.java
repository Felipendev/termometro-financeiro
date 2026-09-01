/**
 * Shared kernel do Termômetro Financeiro.
 *
 * <p>Contém apenas value objects imutáveis, sem dependência de Spring, JPA ou de qualquer
 * outro módulo. Tudo aqui é testável com {@code javac} puro — é o alicerce do resto.
 *
 * <p>Regras que este pacote materializa:
 * <ul>
 *   <li><b>RN-01</b> — saída é negativa, entrada é positiva, sempre em BRL.</li>
 *   <li><b>RN-08</b> — arredondamento para cima em múltiplos (renda extra necessária).</li>
 *   <li><b>RN-10</b> — run-rate: mês parcial nunca compara com mês fechado.</li>
 *   <li><b>RN-14</b> — percentuais de consumo, ritmo e taxa de economia.</li>
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.shared;

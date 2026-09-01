/**
 * Orçamento diário — o Termômetro.
 *
 * <p>Núcleo do MVP: responde "quanto eu posso gastar hoje" e traduz o número em ação. Domínio puro,
 * sem Spring e sem JPA, porque é a regra que o usuário lê todo dia e ela não pode depender de
 * infraestrutura para ser testada.
 *
 * <ul>
 *   <li><b>RN-19</b> — a verba se recalcula todo dia: gastou mais ontem, sobra menos hoje.</li>
 *   <li><b>RN-20</b> — a provisão fica <i>dentro</i> da verba, nunca em cima dela.</li>
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.orcamento.domain;

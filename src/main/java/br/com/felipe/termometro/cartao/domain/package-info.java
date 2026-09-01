/**
 * Cadastro manual de cartão (nome + limite opcional + valor da fatura declarado à mão) — a ponte
 * até o endpoint {@code bills} da Pluggy ser integrado (ver ROADMAP, "spike bills"). Hoje
 * {@code ingestao} já persiste a conta que o sync traz ({@code ContaBancaria}/tabela {@code conta})
 * e calcula gasto real por soma de transação; o que falta é a Felipe poder digitar o valor da
 * fatura à mão para os cartões cuja fatura fechada/ajustada (juros, encargos) o sync ainda não
 * reflete direito.
 *
 * <p>Deliberadamente <b>não</b> uma extensão de {@code ingestao.domain.ContaBancaria}: aquele
 * registro é 100% automático, reescrito por inteiro a cada sync (RN-01) — misturar um valor
 * editado à mão nos mesmos campos seria apagado no próximo sync sem aviso. {@link
 * br.com.felipe.termometro.cartao.domain.Cartao#identificadorContaPluggy()} é só um id solto de
 * correlação com {@code conta.identificador} — nenhuma regra depende de que o vínculo exista.
 */
@org.jspecify.annotations.NullMarked
package br.com.felipe.termometro.cartao.domain;

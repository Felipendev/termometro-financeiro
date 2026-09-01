/**
 * RN-22 — automação e alertas. O domínio de notificação não sabe nada de Telegram, e-mail ou
 * qualquer canal: sabe apenas decidir {@code se} e {@code o quê} avisar, a partir do que os outros
 * domínios (orçamento, classificação) já calcularam. O canal é detalhe de infra.
 */
package br.com.felipe.termometro.notificacao.domain;

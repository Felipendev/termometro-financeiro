package br.com.felipe.termometro.compromissofuturo.domain;

import java.util.List;
import java.util.Set;

/**
 * @param gerados          os compromissos futuros recalculados, prontos para persistir
 * @param seriesProcessadas toda série vista nesta rodada, mesmo a que gerou zero compromissos
 *                          (parcela âncora já é a última) — a infra precisa apagar o que essa
 *                          série tinha gravado antes, senão uma parcela que acabou de ser paga
 *                          deixaria lixo para trás
 */
public record ResultadoDaGeracao(List<CompromissoFuturo> gerados, Set<ChaveDeSerie> seriesProcessadas) {
}

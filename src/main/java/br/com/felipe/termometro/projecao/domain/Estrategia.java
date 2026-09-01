package br.com.felipe.termometro.projecao.domain;

/**
 * RN-09 — como alocar a amortização entre dívidas quando há mais de uma em aberto.
 *
 * <ul>
 *   <li>{@link #AVALANCHE} — maior taxa primeiro; menor custo total em juros.</li>
 *   <li>{@link #BOLA_DE_NEVE} — menor saldo primeiro; primeira dívida zerada mais cedo,
 *       vitória psicológica antes da matemática ótima.</li>
 *   <li>{@link #PROPORCIONAL} — reparte a amortização proporcionalmente ao total devido
 *       (saldo + juros) de cada dívida, via {@link br.com.felipe.termometro.shared.Dinheiro#ratear(java.util.List)}.</li>
 * </ul>
 *
 * <p>Com disponível fixo mês a mês, as três estratégias costumam quitar tudo no mesmo mês — a
 * diferença real está nos juros pagos e no ritmo em que cada dívida individual chega a zero.
 */
public enum Estrategia {
    AVALANCHE,
    BOLA_DE_NEVE,
    PROPORCIONAL
}

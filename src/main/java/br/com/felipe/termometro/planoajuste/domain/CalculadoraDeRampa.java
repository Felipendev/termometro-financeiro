package br.com.felipe.termometro.planoajuste.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-15 — a rampa geométrica de corte. Reduz {@code atual} até {@code alvo} numa progressão
 * geométrica (razão constante mês a mês), nunca superando {@code fatorMaxCorte} de queda de um
 * mês para o outro. Quando o horizonte pedido é curto demais para respeitar esse limite, a rampa
 * é alongada automaticamente até {@link #MESES_MAXIMO} meses.
 *
 * <p><b>Por que {@code double} aqui, e não {@link BigDecimal}.</b> A fórmula usa logaritmo e
 * potenciação fracionária ({@code ln}, {@code x^(1/n)}) — {@code BigDecimal} não tem essas
 * operações nativas, e a alternativa seria depender de uma biblioteca de matemática de precisão
 * arbitrária que este projeto não usa hoje. O erro relativo de {@code double} nessas operações é
 * da ordem de 1e-15 — irrelevante frente aos 2 casas decimais que {@link Dinheiro} já aplica em
 * cada alvo mensal. Decisão documentada porque contraria a preferência geral do projeto por
 * {@code BigDecimal} em dinheiro: o valor intermediário (razão da progressão) não é dinheiro, é
 * um fator adimensional. Validado numericamente (réplica Python) contra os 3 cenários Gherkin e
 * o Anexo B da spec antes de virar este código — bate exato em todos.
 */
public final class CalculadoraDeRampa {

    /** RN-15: "se nem 12 meses forem suficientes... entrega a rampa de 12 meses". */
    public static final int MESES_MAXIMO = 12;

    private CalculadoraDeRampa() { }

    /**
     * @param atual            mediana dos últimos meses fechados — o ponto de partida da rampa
     * @param alvo             o piso (ou meta); precisa ser estritamente menor que {@code atual}
     *                         e estritamente positivo — quem decide se a categoria entra no plano
     *                         (e filtra piso zero) é {@link MotorDoPlanoDeAjuste}, não esta classe
     * @param fatorMaxCorte    fração máxima de queda permitida de um mês para o outro (ex. 0,35)
     * @param mesesSolicitados horizonte pedido pelo usuário; pode ser alongado até
     *                         {@link #MESES_MAXIMO}
     */
    public static ResultadoDaRampa calcular(Dinheiro atual, Dinheiro alvo, BigDecimal fatorMaxCorte,
            int mesesSolicitados) {
        if (!atual.maiorQue(alvo)) {
            throw new IllegalArgumentException(
                    ("atual (%s) deve ser maior que alvo (%s) — quem decide se a categoria entra "
                            + "no plano é o motor, não a calculadora").formatted(atual, alvo));
        }
        if (alvo.ehZero()) {
            throw new IllegalArgumentException(
                    "alvo não pode ser zero — a razão da rampa geométrica colapsaria para zero e o "
                            + "alvo do mês 1 já seria zero, o que não é uma rampa gradual; RN-15 não "
                            + "define esse caso. O motor deve excluir categorias com piso zero antes "
                            + "de chamar esta calculadora.");
        }
        if (mesesSolicitados < 1) {
            throw new IllegalArgumentException("mesesSolicitados deve ser >= 1: " + mesesSolicitados);
        }

        double atualD = atual.valor().doubleValue();
        double alvoD = alvo.valor().doubleValue();
        double fatorD = fatorMaxCorte.doubleValue();

        int nMin = (int) Math.ceil(Math.log(alvoD / atualD) / Math.log(1 - fatorD));
        int n = Math.min(MESES_MAXIMO, Math.max(mesesSolicitados, nMin));
        boolean alongada = n > mesesSolicitados;

        double razao = Math.pow(alvoD / atualD, 1.0 / n);

        List<AlvoMensal> alvos = new ArrayList<>(n);
        double anterior = atualD;
        for (int m = 1; m <= n; m++) {
            double bruto = atualD * Math.pow(razao, m);
            double alvoDoMes = Math.max(alvoD, bruto);
            double reducao = anterior == 0 ? 0 : 1 - (alvoDoMes / anterior);
            alvos.add(new AlvoMensal(m, Dinheiro.de(BigDecimal.valueOf(alvoDoMes)),
                    Percentual.deFracao(BigDecimal.valueOf(reducao))));
            anterior = alvoDoMes;
        }
        // Corrige o arrasto de ponto flutuante: o último mês precisa bater exatamente com o alvo
        // pedido, não com "alvo + 1e-13" nem com um centavo de sobra do arredondamento HALF_EVEN.
        AlvoMensal ultimo = alvos.get(alvos.size() - 1);
        alvos.set(alvos.size() - 1, new AlvoMensal(ultimo.mes(), alvo, ultimo.reducaoPercentual()));

        return new ResultadoDaRampa(n, alongada, alvos);
    }
}

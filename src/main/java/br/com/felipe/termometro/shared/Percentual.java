package br.com.felipe.termometro.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * Percentual guardado como fração ({@code 0,250000} = 25%), com seis casas decimais.
 *
 * <p>Existe para tirar percentual solto em {@code double} das regras de negócio: taxa de
 * economia (RN-14), lift de padrão temporal (RN-13), variação e progresso de meta (RN-10)
 * e taxa máxima de viabilidade (RN-16) são todos comparados contra limiares fixos, e
 * {@code 0.1 + 0.2 != 0.3} em ponto flutuante quebra exatamente esse tipo de comparação.
 *
 * <p>Fração, não pontos: {@code Percentual.dePontos("25")} e
 * {@code Percentual.deFracao("0.25")} produzem o mesmo valor. As duas fábricas existem
 * porque as duas leituras aparecem no domínio, e deixar isso ambíguo gera erro de 100×.
 */
public record Percentual(BigDecimal fracao) implements Comparable<Percentual> {

    public static final int ESCALA = 6;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public static final Percentual ZERO = new Percentual(BigDecimal.ZERO);
    public static final Percentual CEM = new Percentual(BigDecimal.ONE);

    private static final Locale BR = Locale.of("pt", "BR");
    private static final BigDecimal CEM_PONTOS = BigDecimal.valueOf(100);

    public Percentual {
        Objects.requireNonNull(fracao, "fração não pode ser nula");
        fracao = fracao.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static Percentual deFracao(BigDecimal fracao) {
        return new Percentual(fracao);
    }

    public static Percentual deFracao(String fracao) {
        Objects.requireNonNull(fracao, "fração não pode ser nula");
        return new Percentual(new BigDecimal(fracao));
    }

    /** {@code dePontos("25")} → 25%. */
    public static Percentual dePontos(String pontos) {
        Objects.requireNonNull(pontos, "pontos não podem ser nulos");
        return new Percentual(new BigDecimal(pontos)
                .divide(CEM_PONTOS, ESCALA, ARREDONDAMENTO));
    }

    /**
     * Percentual que {@code parte} representa de {@code total}.
     *
     * @throws ArithmeticException se {@code total} for zero (edge case 26)
     */
    public static Percentual deValor(Dinheiro parte, Dinheiro total) {
        Objects.requireNonNull(parte, "parte não pode ser nula");
        return parte.sobre(total);
    }

    public Dinheiro aplicarSobre(Dinheiro base) {
        Objects.requireNonNull(base, "base não pode ser nula");
        return base.multiplicar(fracao);
    }

    public BigDecimal emPontos() {
        return fracao.multiply(CEM_PONTOS).setScale(4, ARREDONDAMENTO);
    }

    public Percentual somar(Percentual outro) {
        Objects.requireNonNull(outro, "parcela não pode ser nula");
        return new Percentual(fracao.add(outro.fracao));
    }

    public Percentual subtrair(Percentual outro) {
        Objects.requireNonNull(outro, "subtraendo não pode ser nulo");
        return new Percentual(fracao.subtract(outro.fracao));
    }

    public boolean ehNegativo() {
        return fracao.signum() < 0;
    }

    public boolean maiorOuIgualA(Percentual outro) {
        return compareTo(outro) >= 0;
    }

    public boolean menorQue(Percentual outro) {
        return compareTo(outro) < 0;
    }

    @Override
    public int compareTo(Percentual outro) {
        return fracao.compareTo(outro.fracao);
    }

    /** {@code "28,6%"} — uma casa decimal, que é o que cabe num rótulo de dashboard. */
    public String formatado() {
        return formatado(1);
    }

    public String formatado(int casas) {
        if (casas < 0) {
            throw new IllegalArgumentException("casas não pode ser negativo: " + casas);
        }
        String padrao = casas == 0 ? "0" : "0." + "0".repeat(casas);
        DecimalFormat formato = new DecimalFormat(padrao, DecimalFormatSymbols.getInstance(BR));
        return formato.format(emPontos()) + "%";
    }

    @Override
    public String toString() {
        return formatado();
    }

    /** Representação canônica para serialização: {@code "0.285714"}. */
    public String paraJson() {
        return fracao.toPlainString();
    }
}

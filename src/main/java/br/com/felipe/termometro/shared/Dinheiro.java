package br.com.felipe.termometro.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Valor monetário em reais, com duas casas decimais e arredondamento HALF_EVEN.
 *
 * <p><b>Convenção de sinal (RN-01):</b> saída é negativa, entrada é positiva.
 *
 * <p><b>Por que BRL-only:</b> a especificação exclui transações em moeda estrangeira sem
 * conversão dos agregados (edge case 16). A conversão acontece no adapter de ingestão, e
 * só chega aqui valor já em reais. Carregar {@code Currency} no value object adicionaria
 * cerimônia em todo o domínio para um caso que o domínio não vê.
 *
 * <p><b>Igualdade:</b> o construtor canônico normaliza a escala para 2, então
 * {@code Dinheiro.de("10.5").equals(Dinheiro.de("10.50"))} é verdadeiro — ao contrário do
 * que aconteceria comparando {@link BigDecimal} diretamente.
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    private static final Locale BR = Locale.of("pt", "BR");
    private static final int ESCALA_INTERMEDIARIA = 10;

    public Dinheiro {
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        valor = valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    // ---------------------------------------------------------------- fábricas

    public static Dinheiro de(String valor) {
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        return new Dinheiro(new BigDecimal(valor));
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public static Dinheiro de(long reais) {
        return new Dinheiro(BigDecimal.valueOf(reais));
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(BigDecimal.valueOf(centavos, ESCALA));
    }

    /**
     * Soma uma coleção. Coleção vazia devolve {@link #ZERO} — nunca {@code null},
     * porque agregado ausente é zero, não desconhecido.
     */
    public static Dinheiro somaDe(Collection<Dinheiro> valores) {
        Objects.requireNonNull(valores, "coleção não pode ser nula");
        return valores.stream().reduce(ZERO, Dinheiro::somar);
    }

    // ------------------------------------------------------------- aritmética

    public Dinheiro somar(Dinheiro outro) {
        Objects.requireNonNull(outro, "parcela não pode ser nula");
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        Objects.requireNonNull(outro, "subtraendo não pode ser nulo");
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public Dinheiro multiplicar(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator não pode ser nulo");
        return new Dinheiro(valor.multiply(fator));
    }

    public Dinheiro multiplicar(long fator) {
        return multiplicar(BigDecimal.valueOf(fator));
    }

    /**
     * Divisão com arredondamento. Para repartir um valor entre N partes <b>sem perder
     * centavos</b>, use {@link #ratear(int)} — este método arredonda e não garante que
     * {@code N × resultado == this}.
     */
    public Dinheiro dividirPor(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "divisor não pode ser nulo");
        if (divisor.signum() == 0) {
            throw new ArithmeticException("divisão por zero em Dinheiro.dividirPor");
        }
        return new Dinheiro(valor.divide(divisor, ESCALA, ARREDONDAMENTO));
    }

    public Dinheiro negado() {
        return new Dinheiro(valor.negate());
    }

    public Dinheiro absoluto() {
        return new Dinheiro(valor.abs());
    }

    // -------------------------------------------------------------- predicados

    public boolean ehZero() {
        return valor.signum() == 0;
    }

    public boolean ehPositivo() {
        return valor.signum() > 0;
    }

    public boolean ehNegativo() {
        return valor.signum() < 0;
    }

    public boolean maiorQue(Dinheiro outro) {
        return compareTo(outro) > 0;
    }

    public boolean menorQue(Dinheiro outro) {
        return compareTo(outro) < 0;
    }

    public boolean maiorOuIgualA(Dinheiro outro) {
        return compareTo(outro) >= 0;
    }

    public boolean menorOuIgualA(Dinheiro outro) {
        return compareTo(outro) <= 0;
    }

    public Dinheiro maximo(Dinheiro outro) {
        return maiorQue(outro) ? this : outro;
    }

    public Dinheiro minimo(Dinheiro outro) {
        return menorQue(outro) ? this : outro;
    }

    // ------------------------------------------------------------- conversões

    /** Valor em centavos. Base de {@code hash_dedupe} (RN-02) e do rateio. */
    public long centavos() {
        return valor.movePointRight(ESCALA).longValueExact();
    }

    /**
     * Percentual que este valor representa de {@code total} (RN-14).
     *
     * @throws ArithmeticException se {@code total} for zero — um consumo sobre meta zero
     *         não é 0% nem 100%, é indefinido, e a regra de negócio precisa tratar isso
     *         explicitamente (edge case 26) em vez de receber um número inventado.
     */
    public Percentual sobre(Dinheiro total) {
        Objects.requireNonNull(total, "total não pode ser nulo");
        if (total.ehZero()) {
            throw new ArithmeticException("percentual sobre total zero é indefinido");
        }
        return Percentual.deFracao(
                valor.divide(total.valor, Percentual.ESCALA, ARREDONDAMENTO));
    }

    // ------------------------------------------------------------ regras (RN)

    /**
     * Arredonda para cima no múltiplo informado (RN-08: renda extra necessária de
     * R$ 380,00 vira R$ 400,00 com múltiplo de R$ 50,00).
     *
     * <p>Arredonda em direção a +infinito: para valores negativos o resultado se aproxima
     * de zero. Na prática a regra sempre recebe o módulo do déficit.
     */
    public Dinheiro arredondarParaCima(Dinheiro multiplo) {
        Objects.requireNonNull(multiplo, "múltiplo não pode ser nulo");
        if (!multiplo.ehPositivo()) {
            throw new IllegalArgumentException(
                    "múltiplo deve ser positivo, recebido: " + multiplo);
        }
        BigDecimal quantidade = valor.divide(multiplo.valor, 0, RoundingMode.CEILING);
        return new Dinheiro(quantidade.multiply(multiplo.valor));
    }

    /**
     * Reparte em {@code partes} iguais <b>sem perder nem inventar centavos</b>:
     * a soma do resultado é sempre exatamente igual a este valor.
     *
     * <p>Os centavos residuais vão para as primeiras partes. R$ 100,00 em 3 partes vira
     * {@code [33,34 · 33,33 · 33,33]}, não três vezes R$ 33,33 com um centavo evaporado.
     */
    public List<Dinheiro> ratear(int partes) {
        if (partes <= 0) {
            throw new IllegalArgumentException("partes deve ser positivo, recebido: " + partes);
        }
        long total = centavos();
        long base = total / partes;
        long resto = total - base * partes;
        long passo = Long.signum(resto);
        long comAjuste = Math.abs(resto);

        List<Dinheiro> resultado = new ArrayList<>(partes);
        for (int i = 0; i < partes; i++) {
            resultado.add(deCentavos(base + (i < comAjuste ? passo : 0)));
        }
        return List.copyOf(resultado);
    }

    /**
     * Reparte proporcionalmente aos pesos, pelo método do maior resto, preservando a soma
     * exata. Usado no rateio de uma transação que cruza o piso humano (RN-05) e na
     * alocação de amortização entre dívidas (RN-09).
     */
    public List<Dinheiro> ratear(List<BigDecimal> pesos) {
        Objects.requireNonNull(pesos, "pesos não podem ser nulos");
        if (pesos.isEmpty()) {
            throw new IllegalArgumentException("é preciso ao menos um peso");
        }
        if (pesos.stream().anyMatch(p -> p == null || p.signum() < 0)) {
            throw new IllegalArgumentException("pesos não podem ser nulos nem negativos");
        }
        BigDecimal somaPesos = pesos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (somaPesos.signum() == 0) {
            throw new IllegalArgumentException("a soma dos pesos não pode ser zero");
        }

        long total = centavos();
        BigDecimal totalBd = BigDecimal.valueOf(total);

        record Fatia(int indice, long base, BigDecimal resto) {}
        List<Fatia> fatias = new ArrayList<>(pesos.size());
        long somaBase = 0;
        for (int i = 0; i < pesos.size(); i++) {
            BigDecimal exato = totalBd.multiply(pesos.get(i))
                    .divide(somaPesos, ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN);
            BigDecimal piso = exato.setScale(0, RoundingMode.FLOOR);
            long base = piso.longValueExact();
            somaBase += base;
            fatias.add(new Fatia(i, base, exato.subtract(piso)));
        }

        long sobra = total - somaBase;
        List<Fatia> porResto = new ArrayList<>(fatias);
        porResto.sort(Comparator.comparing(Fatia::resto).reversed()
                .thenComparingInt(Fatia::indice));

        long[] centavosPorIndice = new long[pesos.size()];
        for (Fatia f : fatias) {
            centavosPorIndice[f.indice()] = f.base();
        }
        for (int i = 0; i < sobra && i < porResto.size(); i++) {
            centavosPorIndice[porResto.get(i).indice()]++;
        }

        List<Dinheiro> resultado = new ArrayList<>(pesos.size());
        for (long c : centavosPorIndice) {
            resultado.add(deCentavos(c));
        }
        return List.copyOf(resultado);
    }

    // ------------------------------------------------------------- comparação

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }

    /** Formato pt-BR legível: {@code "R$ 1.234,56"}, {@code "-R$ 89,90"}. */
    @Override
    public String toString() {
        // DecimalFormat não é thread-safe; instanciar por chamada é aceitável aqui
        // porque toString só é usado em log, mensagem de erro e falha de teste.
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BR));
        return (ehNegativo() ? "-" : "") + "R$ " + formato.format(valor.abs());
    }

    /** Representação canônica para serialização: {@code "1234.56"}, {@code "-89.90"}. */
    public String paraJson() {
        return valor.toPlainString();
    }
}

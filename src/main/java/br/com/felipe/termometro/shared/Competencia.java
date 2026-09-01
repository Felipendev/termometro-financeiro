package br.com.felipe.termometro.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Mês de referência (competência). Envolve {@link YearMonth} para carregar as regras de
 * tempo do domínio — em especial o <b>run-rate</b> da RN-10, que é o guarda-corpo contra
 * o erro de comparar um mês parcial com um mês fechado.
 *
 * <p>Todo método sensível a "hoje" recebe um {@link Clock}. Nada aqui chama
 * {@code LocalDate.now()} sem clock: regra de negócio que depende do relógio do sistema
 * não é testável, e esta é a classe de onde o run-rate inteiro depende.
 *
 * <p>O clock deve carregar {@link #FUSO}. A conversão de UTC para o fuso local acontece
 * antes de chegar aqui (edge case 22: compra às 23h local é 02h UTC do dia seguinte).
 */
public record Competencia(YearMonth valor) implements Comparable<Competencia> {

    public static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final int ESCALA_FATOR = 10;

    public Competencia {
        Objects.requireNonNull(valor, "competência não pode ser nula");
    }

    // ---------------------------------------------------------------- fábricas

    public static Competencia de(int ano, int mes) {
        return new Competencia(YearMonth.of(ano, mes));
    }

    public static Competencia de(YearMonth mes) {
        return new Competencia(mes);
    }

    public static Competencia de(LocalDate data) {
        Objects.requireNonNull(data, "data não pode ser nula");
        return new Competencia(YearMonth.from(data));
    }

    /** Aceita o formato ISO {@code "2026-09"}. */
    public static Competencia parse(String texto) {
        Objects.requireNonNull(texto, "texto não pode ser nulo");
        return new Competencia(YearMonth.parse(texto));
    }

    public static Competencia atual(Clock relogio) {
        Objects.requireNonNull(relogio, "relógio não pode ser nulo");
        return new Competencia(YearMonth.now(relogio));
    }

    // -------------------------------------------------------------- navegação

    public Competencia proxima() {
        return mais(1);
    }

    public Competencia anterior() {
        return menos(1);
    }

    public Competencia mais(int meses) {
        return new Competencia(valor.plusMonths(meses));
    }

    public Competencia menos(int meses) {
        return new Competencia(valor.minusMonths(meses));
    }

    /** Meses de distância, com sinal: negativo se {@code outra} for anterior. */
    public int mesesAte(Competencia outra) {
        Objects.requireNonNull(outra, "competência não pode ser nula");
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(valor, outra.valor);
    }

    /** Intervalo fechado, do menor para o maior. Vazio se {@code fim} for anterior. */
    public Stream<Competencia> ate(Competencia fim) {
        Objects.requireNonNull(fim, "competência final não pode ser nula");
        int quantidade = mesesAte(fim) + 1;
        return quantidade <= 0 ? Stream.empty()
                : Stream.iterate(this, Competencia::proxima).limit(quantidade);
    }

    // ------------------------------------------------------------- calendário

    public LocalDate primeiroDia() {
        return valor.atDay(1);
    }

    public LocalDate ultimoDia() {
        return valor.atEndOfMonth();
    }

    /** 28, 29, 30 ou 31 — nunca 30 fixo (edge case 18). */
    public int quantidadeDeDias() {
        return valor.lengthOfMonth();
    }

    public boolean contem(LocalDate data) {
        Objects.requireNonNull(data, "data não pode ser nula");
        return YearMonth.from(data).equals(valor);
    }

    // ---------------------------------------------------- posição no tempo

    public boolean ehCorrente(Clock relogio) {
        return equals(atual(relogio));
    }

    public boolean ehPassada(Clock relogio) {
        return compareTo(atual(relogio)) < 0;
    }

    public boolean ehFutura(Clock relogio) {
        return compareTo(atual(relogio)) > 0;
    }

    // ------------------------------------------------------- run-rate (RN-10)

    /**
     * Dias já decorridos: o mês inteiro se for passado, o dia do mês se for o corrente,
     * zero se for futuro.
     */
    public int diasDecorridos(Clock relogio) {
        Objects.requireNonNull(relogio, "relógio não pode ser nulo");
        if (ehFutura(relogio)) {
            return 0;
        }
        if (ehPassada(relogio)) {
            return quantidadeDeDias();
        }
        return LocalDate.now(relogio).getDayOfMonth();
    }

    /** Um mês é parcial enquanto não terminou. Todo agregado parcial precisa ser sinalizado. */
    public boolean ehParcial(Clock relogio) {
        return !ehPassada(relogio);
    }

    /**
     * Fator de extrapolação: {@code dias_do_mês / dias_decorridos}.
     *
     * @throws IllegalStateException para competência futura — extrapolar a partir de zero
     *         dia de dados produziria um número sem significado, e devolver zero
     *         silenciosamente faria o mês futuro parecer gasto zero.
     */
    public BigDecimal fatorRunRate(Clock relogio) {
        int decorridos = diasDecorridos(relogio);
        if (decorridos == 0) {
            throw new IllegalStateException(
                    "não há run-rate para competência futura: " + this);
        }
        return BigDecimal.valueOf(quantidadeDeDias())
                .divide(BigDecimal.valueOf(decorridos), ESCALA_FATOR, RoundingMode.HALF_EVEN);
    }

    /**
     * Projeta o total do mês a partir do gasto até agora (RN-10). Num mês fechado o fator
     * é 1 e o valor volta inalterado, o que mantém a chamada segura em qualquer contexto.
     */
    public Dinheiro projetarRunRate(Dinheiro gastoAteAgora, Clock relogio) {
        Objects.requireNonNull(gastoAteAgora, "gasto não pode ser nulo");
        return gastoAteAgora.multiplicar(fatorRunRate(relogio));
    }

    /**
     * Fração do mês já decorrida — denominador do "ritmo" da RN-14.
     * No dia 5 de um mês de 30 dias devolve {@code 0,166667}.
     */
    public Percentual fracaoDecorrida(Clock relogio) {
        int decorridos = diasDecorridos(relogio);
        return Percentual.deFracao(BigDecimal.valueOf(decorridos)
                .divide(BigDecimal.valueOf(quantidadeDeDias()),
                        Percentual.ESCALA, RoundingMode.HALF_EVEN));
    }

    @Override
    public int compareTo(Competencia outra) {
        return valor.compareTo(outra.valor);
    }

    /** {@code "2026-09"} — o mesmo formato aceito por {@link #parse(String)}. */
    @Override
    public String toString() {
        return valor.toString();
    }
}

package br.com.felipe.termometro.vampiros.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * RN-07 — decide se um grupo de cobranças do mesmo estabelecimento (já agrupado por
 * {@code Normalizador.chaveDeEstabelecimento}, fora deste domínio) é uma recorrência:
 *
 * <pre>
 *   ≥ 3 ocorrências, e
 *   intervalo mediano ∈ [26,35] dias (mensal) ou [350,380] dias (anual), e
 *   (max − min) / mediana dos valores ≤ 0,20
 *      — exceção do reajuste: intervalo regular + valores em degrau monotônico
 *        (todo valor antigo abaixo de todo valor novo) → mantém a recorrência,
 *        valor_medio passa a considerar só o patamar atual, marca REAJUSTE_DETECTADO
 * </pre>
 *
 * <p><b>O que a spec não define — e como este código decide:</b> a fórmula de
 * {@code confianca = 0,4·f(n) + 0,3·regularidade + 0,3·estabilidade} está na spec, mas
 * {@code f(n)}, {@code regularidade} e {@code estabilidade} não têm forma fechada. Aqui,
 * {@code f(n) = 1 − 1/(n−1)} (satura suavemente, sem teto artificial) e
 * {@code regularidade}/{@code estabilidade} reaproveitam a mesma métrica do próprio gatilho de
 * elegibilidade — {@code 1 − (max−min)/mediana} — aplicada a intervalo e a valor respectivamente.
 * Validado contra os dois cenários Gherkin da spec (detecção simples com confiança ≥ 0,8 e
 * degrau de reajuste que não quebra a recorrência).
 */
public final class DetectorDeRecorrencias {

    private static final int MINIMO_OCORRENCIAS = 3;
    private static final long MENSAL_MIN_DIAS = 26;
    private static final long MENSAL_MAX_DIAS = 35;
    private static final long ANUAL_MIN_DIAS = 350;
    private static final long ANUAL_MAX_DIAS = 380;

    private static final BigDecimal LIMIAR_VARIACAO = new BigDecimal("0.20");
    private static final Dinheiro LIMIAR_COBRANCA_SILENCIOSA = Dinheiro.de(50);

    private static final BigDecimal PESO_N = new BigDecimal("0.4");
    private static final BigDecimal PESO_REGULARIDADE = new BigDecimal("0.3");
    private static final BigDecimal PESO_ESTABILIDADE = new BigDecimal("0.3");

    private static final int ESCALA_INTERMEDIARIA = 10;

    private DetectorDeRecorrencias() {
    }

    public static Optional<Recorrencia> detectar(String nomeNormalizado, List<Ocorrencia> ocorrencias) {
        Objects.requireNonNull(nomeNormalizado, "nome normalizado não pode ser nulo");
        Objects.requireNonNull(ocorrencias, "ocorrências não podem ser nulas");
        if (ocorrencias.size() < MINIMO_OCORRENCIAS) {
            return Optional.empty();
        }

        List<Ocorrencia> ordenadas = ocorrencias.stream()
                .sorted(Comparator.comparing(Ocorrencia::data))
                .toList();

        List<Long> intervalosDias = intervalosEmDias(ordenadas);
        BigDecimal medianaIntervalo = medianaDeLongs(intervalosDias);
        Optional<Periodicidade> periodicidade = classificar(medianaIntervalo);
        if (periodicidade.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal coefVariacaoIntervalo = coeficienteDeVariacaoDeLongs(intervalosDias, medianaIntervalo);
        boolean intervaloRegular = coefVariacaoIntervalo.compareTo(LIMIAR_VARIACAO) <= 0;

        List<BigDecimal> valores = ordenadas.stream().map(o -> o.valor().valor()).toList();
        BigDecimal medianaValor = mediana(valores);
        BigDecimal coefVariacaoValor = coeficienteDeVariacao(valores, medianaValor);

        boolean reajusteDetectado = false;
        List<BigDecimal> valoresParaMedia = valores;

        if (coefVariacaoValor.compareTo(LIMIAR_VARIACAO) > 0) {
            Optional<Integer> divisor = encontrarDegrauMonotonico(valores);
            if (divisor.isEmpty() || !intervaloRegular) {
                return Optional.empty();
            }
            reajusteDetectado = true;
            valoresParaMedia = valores.subList(divisor.get(), valores.size());
            // a estabilidade do patamar atual, não do histórico inteiro — é o que valor_medio passa a
            // representar depois do reajuste
            BigDecimal medianaPatamar = mediana(valoresParaMedia);
            coefVariacaoValor = coeficienteDeVariacao(valoresParaMedia, medianaPatamar);
        }

        Dinheiro valorMedio = media(valoresParaMedia);
        Dinheiro custoAnual = periodicidade.get() == Periodicidade.MENSAL
                ? valorMedio.multiplicar(12L)
                : valorMedio;

        BigDecimal fDeN = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(BigDecimal.valueOf(ordenadas.size() - 1), ESCALA_INTERMEDIARIA,
                        RoundingMode.HALF_EVEN));
        BigDecimal regularidade = umMenos(coefVariacaoIntervalo);
        BigDecimal estabilidade = umMenos(coefVariacaoValor);

        BigDecimal confiancaFracao = PESO_N.multiply(fDeN)
                .add(PESO_REGULARIDADE.multiply(regularidade))
                .add(PESO_ESTABILIDADE.multiply(estabilidade));

        Recorrencia recorrencia = new Recorrencia(nomeNormalizado, periodicidade.get(), valorMedio,
                custoAnual, Percentual.deFracao(confiancaFracao), ordenadas.getFirst().data(),
                ordenadas.getLast().data(), ordenadas.size(), reajusteDetectado,
                valorMedio.menorQue(LIMIAR_COBRANCA_SILENCIOSA));
        return Optional.of(recorrencia);
    }

    private static List<Long> intervalosEmDias(List<Ocorrencia> ordenadas) {
        List<Long> intervalos = new ArrayList<>(ordenadas.size() - 1);
        for (int i = 1; i < ordenadas.size(); i++) {
            intervalos.add(ChronoUnit.DAYS.between(ordenadas.get(i - 1).data(), ordenadas.get(i).data()));
        }
        return intervalos;
    }

    private static Optional<Periodicidade> classificar(BigDecimal medianaIntervaloDias) {
        if (entre(medianaIntervaloDias, MENSAL_MIN_DIAS, MENSAL_MAX_DIAS)) {
            return Optional.of(Periodicidade.MENSAL);
        }
        if (entre(medianaIntervaloDias, ANUAL_MIN_DIAS, ANUAL_MAX_DIAS)) {
            return Optional.of(Periodicidade.ANUAL);
        }
        return Optional.empty();
    }

    private static boolean entre(BigDecimal valor, long minimo, long maximo) {
        return valor.compareTo(BigDecimal.valueOf(minimo)) >= 0 && valor.compareTo(BigDecimal.valueOf(maximo)) <= 0;
    }

    /**
     * O primeiro ponto de corte, lendo em ordem cronológica, em que todo valor antes dele é
     * estritamente menor que todo valor depois — o degrau do reajuste. Múltiplos cortes válidos
     * são possíveis em séries degeneradas; o primeiro encontrado é o mais conservador (assume o
     * patamar novo mais cedo, não mais tarde).
     */
    private static Optional<Integer> encontrarDegrauMonotonico(List<BigDecimal> valoresEmOrdem) {
        for (int corte = 1; corte < valoresEmOrdem.size(); corte++) {
            BigDecimal maximoAntes = valoresEmOrdem.subList(0, corte).stream()
                    .max(BigDecimal::compareTo).orElseThrow();
            BigDecimal minimoDepois = valoresEmOrdem.subList(corte, valoresEmOrdem.size()).stream()
                    .min(BigDecimal::compareTo).orElseThrow();
            if (maximoAntes.compareTo(minimoDepois) < 0) {
                return Optional.of(corte);
            }
        }
        return Optional.empty();
    }

    private static BigDecimal coeficienteDeVariacaoDeLongs(List<Long> valores, BigDecimal mediana) {
        return coeficienteDeVariacao(valores.stream().map(BigDecimal::valueOf).toList(), mediana);
    }

    private static BigDecimal coeficienteDeVariacao(List<BigDecimal> valores, BigDecimal mediana) {
        if (mediana.signum() == 0) {
            // todas as ocorrências valem zero — sem variação nenhuma pra medir, trata como estável
            return BigDecimal.ZERO;
        }
        BigDecimal maximo = valores.stream().max(BigDecimal::compareTo).orElseThrow();
        BigDecimal minimo = valores.stream().min(BigDecimal::compareTo).orElseThrow();
        return maximo.subtract(minimo).divide(mediana, ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN);
    }

    /** {@code max(0, 1 - coeficiente)} — nunca negativo, mesmo quando a variação passa de 100%. */
    private static BigDecimal umMenos(BigDecimal coeficienteDeVariacao) {
        BigDecimal valor = BigDecimal.ONE.subtract(coeficienteDeVariacao);
        return valor.signum() < 0 ? BigDecimal.ZERO : valor;
    }

    private static BigDecimal mediana(List<BigDecimal> valores) {
        List<BigDecimal> ordenados = valores.stream().sorted().toList();
        int tamanho = ordenados.size();
        int meio = tamanho / 2;
        if (tamanho % 2 == 1) {
            return ordenados.get(meio);
        }
        return ordenados.get(meio - 1).add(ordenados.get(meio))
                .divide(BigDecimal.valueOf(2), ESCALA_INTERMEDIARIA, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal medianaDeLongs(List<Long> valores) {
        return mediana(valores.stream().map(BigDecimal::valueOf).toList());
    }

    private static Dinheiro media(List<BigDecimal> valores) {
        BigDecimal soma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.de(soma.divide(BigDecimal.valueOf(valores.size()), Dinheiro.ESCALA,
                Dinheiro.ARREDONDAMENTO));
    }
}

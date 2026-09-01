package br.com.felipe.termometro.projecao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * RN-09 — motor de projeção: simula a dívida em aberto sendo paga mês a mês, dado o fluxo de
 * caixa esperado, e decide se o horizonte de quitação é viável.
 *
 * <pre>
 *   para cada competência m em [m0 .. m0+horizonte-1]:
 *       entrada     = renda_liquida(m) + renda_extra(m)
 *       saida_fixa  = fixos(m)
 *       saida_var   = variavel(m)
 *       disponivel  = entrada − saida_fixa − saida_var
 *
 *       juros(m)    = Σ_i saldo_i × taxa_i
 *       se disponivel &lt;= 0:
 *           amortizacao = 0; saldo_i += juros_i               -- mês apertado
 *       senão:
 *           amortizacao = min(disponivel, Σ saldo_i + Σ juros_i)
 *           aloca(amortizacao) conforme estratégia
 *           reserva += disponivel − amortizacao
 * </pre>
 *
 * <p>Se a dívida não quita dentro do horizonte pedido, o motor faz busca binária pela menor
 * renda extra mensal constante que quita em até {@value #HORIZONTE_BUSCA_BINARIA_MESES} meses,
 * e devolve {@code StatusProjecao.INVIAVEL} com essa sugestão.
 *
 * <p><b>Nota sobre "aloca(amortizacao) conforme estratégia":</b> este motor interpreta a
 * alocação como sequencial com transbordo dentro do mesmo mês — a dívida prioritária (maior
 * taxa na avalanche, menor saldo na bola de neve) recebe o quanto couber do seu saldo (que já
 * incorpora o juro do mês, pago ou não — ver {@link #alocarAmortizacao}), e o que sobra
 * transborda para a próxima dívida na ordem, no mesmo mês. Essa leitura reproduz exatamente os
 * três números do Anexo B: busca binária de R$ 1.245,46/mês (dívida de R$ 20.000 a 3,5% a.m.
 * em 24 meses), e o cenário avalanche/bola-de-neve (A 8k@3,5% / B 2k@1,2%, disponível 1.500) —
 * ambas quitam em 8 meses, juros totais R$ 1.191,48 na avalanche contra R$ 1.517,22 na bola de
 * neve.
 */
public final class MotorDeProjecao {

    private static final int HORIZONTE_BUSCA_BINARIA_MESES = 24;
    private static final long TETO_BUSCA_CENTAVOS = 100_000_000_000L;

    private MotorDeProjecao() {
    }

    public static Projecao projetar(Competencia inicio, int horizonteMeses,
            Function<Competencia, Dinheiro> rendaLiquida, Function<Competencia, Dinheiro> rendaExtra,
            Function<Competencia, Dinheiro> saidaFixa, Function<Competencia, Dinheiro> saidaVariavel,
            List<SaldoInicialDeDivida> dividas, Estrategia estrategia, int reservaAlvoMeses) {
        Objects.requireNonNull(inicio, "competência inicial não pode ser nula");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(rendaExtra, "renda extra não pode ser nula");
        Objects.requireNonNull(saidaFixa, "saída fixa não pode ser nula");
        Objects.requireNonNull(saidaVariavel, "saída variável não pode ser nula");
        Objects.requireNonNull(dividas, "dívidas não podem ser nulas");
        Objects.requireNonNull(estrategia, "estratégia não pode ser nula");
        if (horizonteMeses <= 0) {
            throw new IllegalArgumentException("horizonte deve ser positivo, recebido: " + horizonteMeses);
        }
        if (reservaAlvoMeses < 0) {
            throw new IllegalArgumentException(
                    "meses de reserva alvo não pode ser negativo, recebido: " + reservaAlvoMeses);
        }

        Resultado resultado = simular(inicio, horizonteMeses, rendaLiquida, rendaExtra, saidaFixa,
                saidaVariavel, dividas, estrategia, reservaAlvoMeses);

        if (resultado.marcos().dataQuitacao() != null) {
            StatusProjecao status = resultado.meses().stream().anyMatch(MesProjetado::apertado)
                    ? StatusProjecao.VIAVEL_COM_APERTO
                    : StatusProjecao.VIAVEL;
            return new Projecao(inicio, estrategia, resultado.meses(), resultado.marcos(), status, null);
        }

        Dinheiro rendaExtraMinima = buscarRendaExtraMinima(inicio, rendaLiquida, rendaExtra, saidaFixa,
                saidaVariavel, dividas, estrategia, reservaAlvoMeses);
        return new Projecao(inicio, estrategia, resultado.meses(), resultado.marcos(), StatusProjecao.INVIAVEL,
                rendaExtraMinima);
    }

    // ---------------------------------------------------------- busca binária

    /**
     * Menor renda extra mensal constante que quita todas as dívidas em até
     * {@value #HORIZONTE_BUSCA_BINARIA_MESES} meses. Busca binária em centavos — válida porque
     * "quita dentro do horizonte" é monótona em renda extra (é o próprio invariante RN-09:
     * mais renda extra nunca atrasa a quitação).
     */
    private static Dinheiro buscarRendaExtraMinima(Competencia inicio,
            Function<Competencia, Dinheiro> rendaLiquida, Function<Competencia, Dinheiro> rendaExtraBase,
            Function<Competencia, Dinheiro> saidaFixa, Function<Competencia, Dinheiro> saidaVariavel,
            List<SaldoInicialDeDivida> dividas, Estrategia estrategia, int reservaAlvoMeses) {

        long baixoCentavos = 0;
        long altoCentavos = 1;
        while (!quitaDentroDoHorizonte(Dinheiro.deCentavos(altoCentavos), inicio, rendaLiquida,
                rendaExtraBase, saidaFixa, saidaVariavel, dividas, estrategia, reservaAlvoMeses)) {
            baixoCentavos = altoCentavos;
            altoCentavos *= 4;
            if (altoCentavos > TETO_BUSCA_CENTAVOS) {
                throw new IllegalStateException(
                        "renda extra mínima não converge — verifique taxas de juros e horizonte");
            }
        }

        while (altoCentavos - baixoCentavos > 1) {
            long meioCentavos = baixoCentavos + (altoCentavos - baixoCentavos) / 2;
            Dinheiro meio = Dinheiro.deCentavos(meioCentavos);
            if (quitaDentroDoHorizonte(meio, inicio, rendaLiquida, rendaExtraBase, saidaFixa, saidaVariavel,
                    dividas, estrategia, reservaAlvoMeses)) {
                altoCentavos = meioCentavos;
            } else {
                baixoCentavos = meioCentavos;
            }
        }
        return Dinheiro.deCentavos(altoCentavos);
    }

    private static boolean quitaDentroDoHorizonte(Dinheiro rendaExtraAdicional, Competencia inicio,
            Function<Competencia, Dinheiro> rendaLiquida, Function<Competencia, Dinheiro> rendaExtraBase,
            Function<Competencia, Dinheiro> saidaFixa, Function<Competencia, Dinheiro> saidaVariavel,
            List<SaldoInicialDeDivida> dividas, Estrategia estrategia, int reservaAlvoMeses) {
        Function<Competencia, Dinheiro> rendaExtraComAdicional =
                m -> rendaExtraBase.apply(m).somar(rendaExtraAdicional);
        Resultado resultado = simular(inicio, HORIZONTE_BUSCA_BINARIA_MESES, rendaLiquida,
                rendaExtraComAdicional, saidaFixa, saidaVariavel, dividas, estrategia, reservaAlvoMeses);
        return resultado.marcos().dataQuitacao() != null;
    }

    // ------------------------------------------------------------- simulação

    private record Resultado(List<MesProjetado> meses, Marcos marcos) {
    }

    private static Resultado simular(Competencia inicio, int horizonteMeses,
            Function<Competencia, Dinheiro> rendaLiquida, Function<Competencia, Dinheiro> rendaExtra,
            Function<Competencia, Dinheiro> saidaFixa, Function<Competencia, Dinheiro> saidaVariavel,
            List<SaldoInicialDeDivida> dividas, Estrategia estrategia, int reservaAlvoMeses) {

        List<EstadoDivida> estados = new ArrayList<>(dividas.size());
        for (SaldoInicialDeDivida dividaInicial : dividas) {
            estados.add(new EstadoDivida(dividaInicial.saldoDevedor(), dividaInicial.taxaJurosMensal()));
        }

        List<MesProjetado> meses = new ArrayList<>(horizonteMeses);
        Dinheiro reserva = Dinheiro.ZERO;
        Dinheiro jurosTotaisPagos = Dinheiro.ZERO;
        @Nullable Competencia dataQuitacao = null;
        @Nullable Competencia primeiroRealGuardado = null;
        @Nullable Competencia reservaCompleta = null;

        Competencia m = inicio;
        for (int i = 0; i < horizonteMeses; i++, m = m.proxima()) {
            Dinheiro entrada = rendaLiquida.apply(m).somar(rendaExtra.apply(m));
            Dinheiro saidaFixaM = saidaFixa.apply(m);
            Dinheiro saidaVariavelM = saidaVariavel.apply(m);
            Dinheiro disponivel = entrada.subtrair(saidaFixaM).subtrair(saidaVariavelM);

            List<Dinheiro> jurosDoMes = new ArrayList<>(estados.size());
            Dinheiro jurosTotalDoMes = Dinheiro.ZERO;
            for (EstadoDivida estado : estados) {
                Dinheiro jurosDaDivida = estado.saldo.ehPositivo()
                        ? estado.taxa.aplicarSobre(estado.saldo)
                        : Dinheiro.ZERO;
                jurosDoMes.add(jurosDaDivida);
                jurosTotalDoMes = jurosTotalDoMes.somar(jurosDaDivida);
            }
            jurosTotaisPagos = jurosTotaisPagos.somar(jurosTotalDoMes);

            boolean apertado;
            Dinheiro amortizacaoDoMes;
            if (!disponivel.ehPositivo()) {
                apertado = true;
                amortizacaoDoMes = Dinheiro.ZERO;
                for (int idx = 0; idx < estados.size(); idx++) {
                    EstadoDivida estado = estados.get(idx);
                    estado.saldo = estado.saldo.somar(jurosDoMes.get(idx));
                }
            } else {
                apertado = false;
                Dinheiro totalDevido = Dinheiro.ZERO;
                for (int idx = 0; idx < estados.size(); idx++) {
                    totalDevido = totalDevido.somar(estados.get(idx).saldo).somar(jurosDoMes.get(idx));
                }
                amortizacaoDoMes = disponivel.minimo(totalDevido);
                alocarAmortizacao(estados, jurosDoMes, amortizacaoDoMes, estrategia);
                reserva = reserva.somar(disponivel.subtrair(amortizacaoDoMes));
            }

            Dinheiro saldoDividaFimDoMes = Dinheiro.somaDe(estados.stream().map(e -> e.saldo).toList());

            meses.add(new MesProjetado(m, entrada, saidaFixaM, saidaVariavelM, disponivel, jurosTotalDoMes,
                    amortizacaoDoMes, reserva, saldoDividaFimDoMes, apertado));

            if (dataQuitacao == null && saldoDividaFimDoMes.ehZero()) {
                dataQuitacao = m;
            }
            if (primeiroRealGuardado == null && reserva.ehPositivo()) {
                primeiroRealGuardado = m;
            }
            if (reservaCompleta == null && reservaAlvoMeses > 0) {
                Dinheiro alvo = saidaVariavelM.somar(saidaFixaM).multiplicar((long) reservaAlvoMeses);
                if (reserva.maiorOuIgualA(alvo)) {
                    reservaCompleta = m;
                }
            }
        }

        @Nullable Integer mesesAteQuitacao = dataQuitacao == null ? null : inicio.mesesAte(dataQuitacao) + 1;
        Marcos marcos = new Marcos(dataQuitacao, primeiroRealGuardado, reservaCompleta, jurosTotaisPagos,
                mesesAteQuitacao);
        return new Resultado(List.copyOf(meses), marcos);
    }

    // -------------------------------------------------------------- alocação

    /**
     * Aloca a amortização do mês. Antes de qualquer pagamento, todo saldo incorpora o juro do
     * mês — pago ou não: uma dívida que não chega a receber amortização neste mês (porque a
     * ordem da estratégia esgotou {@code amortizacaoDoMes} antes de chegar nela) ainda assim
     * teve seu saldo corrigido pelos juros já contabilizados em {@code jurosDoMes} — sem isso o
     * juro entraria em {@code jurosTotaisPagos} sem nunca aparecer no saldo da dívida, quebrando
     * a reconciliação centavo a centavo.
     */
    private static void alocarAmortizacao(List<EstadoDivida> estados, List<Dinheiro> jurosDoMes,
            Dinheiro amortizacaoDoMes, Estrategia estrategia) {
        if (estados.isEmpty()) {
            return;
        }
        List<Integer> ordem = ordenarParaAlocacao(estados, estrategia);

        for (int idx = 0; idx < estados.size(); idx++) {
            EstadoDivida estado = estados.get(idx);
            estado.saldo = estado.saldo.somar(jurosDoMes.get(idx));
        }

        if (amortizacaoDoMes.ehZero()) {
            return;
        }
        if (estrategia == Estrategia.PROPORCIONAL) {
            alocarProporcional(estados, amortizacaoDoMes, ordem);
            return;
        }

        Dinheiro restante = amortizacaoDoMes;
        for (int idx : ordem) {
            if (restante.ehZero()) {
                break;
            }
            EstadoDivida estado = estados.get(idx);
            if (!estado.saldo.ehPositivo()) {
                continue;
            }
            Dinheiro pago = restante.minimo(estado.saldo);
            estado.saldo = estado.saldo.subtrair(pago);
            restante = restante.subtrair(pago);
        }
    }

    private static List<Integer> ordenarParaAlocacao(List<EstadoDivida> estados, Estrategia estrategia) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < estados.size(); i++) {
            indices.add(i);
        }
        Comparator<Integer> comparador = switch (estrategia) {
            case AVALANCHE -> Comparator.<Integer, Percentual>comparing(i -> estados.get(i).taxa).reversed();
            case BOLA_DE_NEVE -> Comparator.comparing(i -> estados.get(i).saldo);
            case PROPORCIONAL -> Comparator.comparingInt(i -> i);
        };
        indices.sort(comparador);
        return indices;
    }

    /**
     * Reparte a amortização proporcionalmente ao saldo de cada dívida ativa — que a esta altura
     * já incorpora o juro do mês (ver {@link #alocarAmortizacao}) — via
     * {@link Dinheiro#ratear(List)}, que preserva o total exato por construção. Como o peso de
     * cada dívida é o próprio saldo que ela deve, e a amortização nunca excede a soma dos
     * saldos, a fatia de cada dívida nunca deveria superar o que ela deve; o {@code minimo}
     * abaixo é só uma salvaguarda contra um resíduo de arredondamento no limite — o invariante
     * "saldo nunca negativo" não pode depender de uma conta que "deveria" fechar.
     */
    private static void alocarProporcional(List<EstadoDivida> estados, Dinheiro amortizacaoDoMes,
            List<Integer> ordem) {
        List<Integer> ativos = new ArrayList<>();
        List<BigDecimal> pesos = new ArrayList<>();
        for (int idx : ordem) {
            Dinheiro saldo = estados.get(idx).saldo;
            if (saldo.ehPositivo()) {
                ativos.add(idx);
                pesos.add(saldo.valor());
            }
        }
        if (ativos.isEmpty()) {
            return;
        }
        List<Dinheiro> fatias = amortizacaoDoMes.ratear(pesos);
        for (int i = 0; i < ativos.size(); i++) {
            int idx = ativos.get(i);
            EstadoDivida estado = estados.get(idx);
            Dinheiro pago = fatias.get(i).minimo(estado.saldo);
            estado.saldo = estado.saldo.subtrair(pago);
        }
    }

    private static final class EstadoDivida {
        private Dinheiro saldo;
        private final Percentual taxa;

        private EstadoDivida(Dinheiro saldo, Percentual taxa) {
            this.saldo = saldo;
            this.taxa = taxa;
        }
    }
}

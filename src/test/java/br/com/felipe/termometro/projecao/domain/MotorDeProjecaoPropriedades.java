package br.com.felipe.termometro.projecao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.LongRange;

/**
 * Invariantes de {@link MotorDeProjecao} (RN-09) — as quatro propriedades que a spec lista
 * explicitamente: saldo de dívida nunca negativo, reconciliação centavo a centavo entre
 * amortização + juros e a variação do saldo, reserva monotonicamente não decrescente sem
 * saque, e mais renda extra nunca atrasa a quitação.
 */
@Label("MotorDeProjecao — propriedades")
class MotorDeProjecaoPropriedades {

    private static final Competencia INICIO = Competencia.de(2026, 9);
    private static final int HORIZONTE = 18;

    @Provide
    Arbitrary<Dinheiro> saldoRazoavel() {
        return Arbitraries.longs().between(1_000L, 5_000_000L).map(Dinheiro::deCentavos);
    }

    @Provide
    Arbitrary<Percentual> taxaRazoavel() {
        return Arbitraries.longs().between(0L, 10_00L).map(pontosCentesimais ->
                Percentual.deFracao(java.math.BigDecimal.valueOf(pontosCentesimais, 4)));
    }

    @Provide
    Arbitrary<SaldoInicialDeDivida> dividaRazoavel() {
        return Arbitraries.integers().between(1, 999).flatMap(indice ->
                saldoRazoavel().flatMap(saldo ->
                        taxaRazoavel().map(taxa ->
                                new SaldoInicialDeDivida("dívida-" + indice, saldo, taxa))));
    }

    @Provide
    Arbitrary<List<SaldoInicialDeDivida>> dividasRazoaveis() {
        return dividaRazoavel().list().ofMinSize(0).ofMaxSize(3);
    }

    @Provide
    Arbitrary<Dinheiro> disponivelBaseRazoavel() {
        return Arbitraries.longs().between(-100_000L, 300_000L).map(Dinheiro::deCentavos);
    }

    @Provide
    Arbitrary<Estrategia> estrategiaQualquer() {
        return Arbitraries.of(Estrategia.values());
    }

    private Projecao projetar(List<SaldoInicialDeDivida> dividas, Dinheiro disponivelBase,
            Estrategia estrategia, Dinheiro rendaExtra, int horizonte) {
        return MotorDeProjecao.projetar(INICIO, horizonte, m -> disponivelBase, m -> rendaExtra,
                m -> Dinheiro.ZERO, m -> Dinheiro.ZERO, dividas, estrategia, 0);
    }

    @Property(tries = 200)
    @Label("saldo de dívida nunca é negativo em nenhum mês")
    void saldoNuncaNegativo(@ForAll("dividasRazoaveis") List<SaldoInicialDeDivida> dividas,
            @ForAll("disponivelBaseRazoavel") Dinheiro disponivelBase,
            @ForAll("estrategiaQualquer") Estrategia estrategia) {
        Projecao projecao = projetar(dividas, disponivelBase, estrategia, Dinheiro.ZERO, HORIZONTE);
        assertThat(projecao.meses())
                .allSatisfy(mes -> assertThat(mes.saldoDividaFimDoMes().ehNegativo()).isFalse());
    }

    @Property(tries = 200)
    @Label("saldo total reconcilia com juros e amortização até o centavo, mês a mês")
    void reconciliacaoDeCentavos(@ForAll("dividasRazoaveis") List<SaldoInicialDeDivida> dividas,
            @ForAll("disponivelBaseRazoavel") Dinheiro disponivelBase,
            @ForAll("estrategiaQualquer") Estrategia estrategia) {
        Projecao projecao = projetar(dividas, disponivelBase, estrategia, Dinheiro.ZERO, HORIZONTE);

        Dinheiro saldoAnterior = Dinheiro.somaDe(dividas.stream().map(SaldoInicialDeDivida::saldoDevedor).toList());
        for (MesProjetado mes : projecao.meses()) {
            Dinheiro esperado = saldoAnterior.somar(mes.juros()).subtrair(mes.amortizacao());
            assertThat(mes.saldoDividaFimDoMes()).isEqualTo(esperado);
            saldoAnterior = mes.saldoDividaFimDoMes();
        }
    }

    @Property(tries = 200)
    @Label("reserva nunca decresce mês a mês")
    void reservaNuncaDecresce(@ForAll("dividasRazoaveis") List<SaldoInicialDeDivida> dividas,
            @ForAll("disponivelBaseRazoavel") Dinheiro disponivelBase,
            @ForAll("estrategiaQualquer") Estrategia estrategia) {
        Projecao projecao = projetar(dividas, disponivelBase, estrategia, Dinheiro.ZERO, HORIZONTE);

        List<MesProjetado> meses = projecao.meses();
        for (int i = 1; i < meses.size(); i++) {
            assertThat(meses.get(i).reservaAcumulada())
                    .isGreaterThanOrEqualTo(meses.get(i - 1).reservaAcumulada());
        }
    }

    @Property(tries = 80)
    @Label("mais renda extra nunca atrasa a quitação")
    void maisRendaExtraNuncaAtrasaQuitacao(@ForAll("dividasRazoaveis") List<SaldoInicialDeDivida> dividas,
            @ForAll("disponivelBaseRazoavel") Dinheiro disponivelBase,
            @ForAll("estrategiaQualquer") Estrategia estrategia,
            @ForAll @LongRange(min = 0, max = 200_000) long extraCentavos) {
        Dinheiro extra = Dinheiro.deCentavos(extraCentavos);

        Projecao semExtra = projetar(dividas, disponivelBase, estrategia, Dinheiro.ZERO, HORIZONTE);
        Projecao comExtra = projetar(dividas, disponivelBase, estrategia, extra, HORIZONTE);

        Integer mesesSemExtra = semExtra.marcos().mesesAteQuitacao();
        Integer mesesComExtra = comExtra.marcos().mesesAteQuitacao();

        if (mesesSemExtra != null) {
            assertThat(mesesComExtra).isNotNull();
            assertThat(mesesComExtra).isLessThanOrEqualTo(mesesSemExtra);
        }
        // se mesesSemExtra é null (não quitou dentro do horizonte), comExtra pode quitar ou não —
        // o único resultado proibido é quitar mais tarde do que sem o extra.
    }
}

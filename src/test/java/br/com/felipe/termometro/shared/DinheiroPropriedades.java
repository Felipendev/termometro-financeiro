package br.com.felipe.termometro.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Assume;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Invariantes de {@link Dinheiro}. Estas são as propriedades que precisam valer para
 * <i>qualquer</i> entrada — o motor de projeção (RN-09) e o rateio do piso humano (RN-05)
 * dependem delas, e um exemplo bem escolhido não prova nenhuma.
 */
@Label("Dinheiro — propriedades")
class DinheiroPropriedades {

    @Provide
    Arbitrary<Dinheiro> dinheiro() {
        return Arbitraries.longs().between(-100_000_000L, 100_000_000L).map(Dinheiro::deCentavos);
    }

    @Provide
    Arbitrary<Dinheiro> dinheiroPositivo() {
        return Arbitraries.longs().between(1L, 100_000_000L).map(Dinheiro::deCentavos);
    }

    @Property
    @Label("ratear nunca perde nem inventa centavo")
    void rateioPreservaOTotal(@ForAll("dinheiro") Dinheiro valor, @ForAll @IntRange(min = 1, max = 60) int partes) {
        assertThat(Dinheiro.somaDe(valor.ratear(partes))).isEqualTo(valor);
    }

    @Property
    @Label("as partes de um rateio diferem no máximo um centavo entre si")
    void rateioEhEquilibrado(@ForAll("dinheiro") Dinheiro valor, @ForAll @IntRange(min = 1, max = 60) int partes) {
        List<Dinheiro> fatias = valor.ratear(partes);
        long maior = fatias.stream().mapToLong(Dinheiro::centavos).max().orElseThrow();
        long menor = fatias.stream().mapToLong(Dinheiro::centavos).min().orElseThrow();
        assertThat(maior - menor).isLessThanOrEqualTo(1L);
    }

    @Provide
    Arbitrary<List<BigDecimal>> pesos() {
        return Arbitraries.longs().between(0L, 10_000L)
                .map(BigDecimal::valueOf)
                .list().ofMinSize(1).ofMaxSize(12);
    }

    @Property
    @Label("rateio por pesos também preserva o total")
    void rateioPorPesosPreservaOTotal(@ForAll("dinheiro") Dinheiro valor,
                                      @ForAll("pesos") List<BigDecimal> pesos) {
        // soma de pesos zero é rejeitada por contrato — coberta por exemplo em DinheiroTest
        Assume.that(pesos.stream().anyMatch(peso -> peso.signum() > 0));
        assertThat(Dinheiro.somaDe(valor.ratear(pesos))).isEqualTo(valor);
    }

    @Property
    @Label("somar e subtrair o mesmo valor devolve o original")
    void somaESubtracaoSaoInversas(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b) {
        assertThat(a.somar(b).subtrair(b)).isEqualTo(a);
    }

    @Property
    @Label("a soma é comutativa")
    void somaEhComutativa(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b) {
        assertThat(a.somar(b)).isEqualTo(b.somar(a));
    }

    @Property
    @Label("negar duas vezes devolve o original")
    void negacaoEhInvolutiva(@ForAll("dinheiro") Dinheiro a) {
        assertThat(a.negado().negado()).isEqualTo(a);
    }

    @Property
    @Label("arredondar para cima nunca reduz o valor e sempre cai num múltiplo")
    void arredondamentoParaCima(@ForAll("dinheiroPositivo") Dinheiro valor,
                                @ForAll("dinheiroPositivo") Dinheiro multiplo) {
        Dinheiro arredondado = valor.arredondarParaCima(multiplo);
        assertThat(arredondado).isGreaterThanOrEqualTo(valor);
        assertThat(arredondado.centavos() % multiplo.centavos()).isZero();
        assertThat(arredondado.subtrair(valor)).isLessThan(multiplo);
    }

    @Property
    @Label("centavos e deCentavos são inversos")
    void conversaoDeCentavosEhReversivel(@ForAll("dinheiro") Dinheiro a) {
        assertThat(Dinheiro.deCentavos(a.centavos())).isEqualTo(a);
    }

    @Property
    @Label("a ordem total é consistente com a diferença")
    void ordemEhConsistente(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b) {
        assertThat(a.maiorQue(b)).isEqualTo(a.subtrair(b).ehPositivo());
        assertThat(a.compareTo(b) == 0).isEqualTo(a.equals(b));
    }
}

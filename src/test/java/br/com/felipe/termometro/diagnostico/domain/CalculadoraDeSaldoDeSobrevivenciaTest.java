package br.com.felipe.termometro.diagnostico.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalculadoraDeSaldoDeSobrevivencia (RN-08)")
class CalculadoraDeSaldoDeSobrevivenciaTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Test
    @DisplayName("déficit gera renda extra necessária arredondada para cima — cenário do Gherkin da spec")
    void deficitGeraRendaExtraArredondada() {
        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(SETEMBRO,
                Dinheiro.de("7400.00"), Dinheiro.de("4100.00"), Dinheiro.ZERO,
                Dinheiro.de("2300.00"), Dinheiro.de("1380.00"));

        assertThat(saldo.totalComprometido()).isEqualTo(Dinheiro.de("7780.00"));
        assertThat(saldo.saldo()).isEqualTo(Dinheiro.de("-380.00"));
        assertThat(saldo.deficit()).isTrue();
        assertThat(saldo.rendaExtraNecessaria()).isEqualTo(Dinheiro.de("400.00"));
    }

    @Test
    @DisplayName("compromissos futuros do mês (parcelas de cartão) somam em cima do custo fixo")
    void compromissosFuturosSomamNoComprometidoFixo() {
        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(SETEMBRO,
                Dinheiro.de(10000), Dinheiro.de("4264.05"), Dinheiro.de("1837.00"),
                Dinheiro.de("1310.00"), Dinheiro.de("2058.05"));

        assertThat(saldo.comprometidoFixo()).isEqualTo(Dinheiro.de("6101.05"));
        assertThat(saldo.totalComprometido()).isEqualTo(Dinheiro.de("9469.10"));
        assertThat(saldo.saldo()).isEqualTo(Dinheiro.de("530.90"));
        assertThat(saldo.deficit()).isFalse();
        assertThat(saldo.rendaExtraNecessaria()).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    @DisplayName("saldo positivo não gera renda extra")
    void saldoPositivoNaoGeraRendaExtra() {
        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(SETEMBRO,
                Dinheiro.de(10000), Dinheiro.de(3000), Dinheiro.ZERO, Dinheiro.de(1000), Dinheiro.ZERO);

        assertThat(saldo.deficit()).isFalse();
        assertThat(saldo.rendaExtraNecessaria()).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    @DisplayName("saldo exatamente zero não é déficit")
    void saldoZeroNaoEhDeficit() {
        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(SETEMBRO,
                Dinheiro.de(10000), Dinheiro.de(10000), Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO);

        assertThat(saldo.deficit()).isFalse();
        assertThat(saldo.rendaExtraNecessaria()).isEqualTo(Dinheiro.ZERO);
    }

    @Test
    @DisplayName("déficit exato num múltiplo de 50 não sobe pro próximo múltiplo")
    void deficitExatoNoMultiploNaoArredondaPraCima() {
        SaldoDeSobrevivencia saldo = CalculadoraDeSaldoDeSobrevivencia.calcular(SETEMBRO,
                Dinheiro.de(9900), Dinheiro.de(10000), Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO);

        assertThat(saldo.saldo()).isEqualTo(Dinheiro.de(-100));
        assertThat(saldo.rendaExtraNecessaria()).isEqualTo(Dinheiro.de(100));
    }
}

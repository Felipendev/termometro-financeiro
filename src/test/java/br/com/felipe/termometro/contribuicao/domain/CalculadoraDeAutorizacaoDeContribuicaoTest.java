package br.com.felipe.termometro.contribuicao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** RN-28.1 — os 3 cenários da spec: sem espaço, espaço surge, e o alvo já atingido. */
class CalculadoraDeAutorizacaoDeContribuicaoTest {

    private static final Competencia PROXIMA = Competencia.de(2026, 10);
    private static final Dinheiro RENDA = Dinheiro.de("10000");
    private static final Dinheiro COLCHAO = Dinheiro.de("1310"); // MinimoVariavel real do Felipe

    private static MetaContribuicao meta(String atual, String alvo) {
        return new MetaContribuicao(NomeDaContribuicao.DIZIMO,
                Percentual.deFracao(alvo), Percentual.deFracao(atual), Percentual.deFracao("0.02"));
    }

    @Test
    void semEspacoMantemOPercentualAtual() {
        MetaContribuicao meta = meta("0.02", "0.10");
        Dinheiro saldoProjetado = Dinheiro.de("1400"); // abaixo do colchão depois da contribuição atual
        Dinheiro contribuicaoAtual = Percentual.deFracao("0.02").aplicarSobre(RENDA); // 200

        Optional<ProximoPassoContribuicao> passo = CalculadoraDeAutorizacaoDeContribuicao.avalia(
                meta, PROXIMA, saldoProjetado, RENDA, contribuicaoAtual, COLCHAO);

        assertThat(passo).isEmpty();
    }

    @Test
    void espacoSurgePropoeOProximoPasso() {
        MetaContribuicao meta = meta("0.02", "0.10");
        Dinheiro saldoProjetado = Dinheiro.de("3000"); // bem acima do colchão
        Dinheiro contribuicaoAtual = Percentual.deFracao("0.02").aplicarSobre(RENDA); // 200

        Optional<ProximoPassoContribuicao> passo = CalculadoraDeAutorizacaoDeContribuicao.avalia(
                meta, PROXIMA, saldoProjetado, RENDA, contribuicaoAtual, COLCHAO);

        assertThat(passo).isPresent();
        assertThat(passo.get().percentualProposto()).isEqualTo(Percentual.deFracao("0.04"));
        assertThat(passo.get().valorProposto()).isEqualTo(Dinheiro.de("400"));
    }

    @Test
    void naoUltrapassaOAlvoMesmoComMuitoEspaco() {
        MetaContribuicao meta = meta("0.09", "0.10"); // passo de 2 pontos passaria de 11%
        Dinheiro saldoProjetado = Dinheiro.de("5000");
        Dinheiro contribuicaoAtual = Percentual.deFracao("0.09").aplicarSobre(RENDA);

        Optional<ProximoPassoContribuicao> passo = CalculadoraDeAutorizacaoDeContribuicao.avalia(
                meta, PROXIMA, saldoProjetado, RENDA, contribuicaoAtual, COLCHAO);

        assertThat(passo).isPresent();
        assertThat(passo.get().percentualProposto()).isEqualTo(Percentual.deFracao("0.10"));
    }

    @Test
    void metaQueJaAtingiuOAlvoNuncaPropoeNovoPasso() {
        MetaContribuicao meta = meta("0.10", "0.10");
        Dinheiro saldoProjetado = Dinheiro.de("5000");

        Optional<ProximoPassoContribuicao> passo = CalculadoraDeAutorizacaoDeContribuicao.avalia(
                meta, PROXIMA, saldoProjetado, RENDA, Dinheiro.de("1000"), COLCHAO);

        assertThat(passo).isEmpty();
    }
}

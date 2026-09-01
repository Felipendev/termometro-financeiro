package br.com.felipe.termometro.triagem.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** RN-29 — cada combinação de etiqueta/natureza precisa cair no grupo certo. */
class DerivadorDePapelNaVidaTest {

    @Test
    void azulComFixoEEssencial() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.AZUL, Natureza.FIXO))
                .contains(PapelNaVida.ESSENCIAL);
    }

    @Test
    void azulComVariavelEImportanteAjustavel() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.AZUL, Natureza.VARIAVEL))
                .contains(PapelNaVida.IMPORTANTE_AJUSTAVEL);
    }

    @Test
    void amarelaEReduzivelIndependenteDaNatureza() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.AMARELA, Natureza.VARIAVEL))
                .contains(PapelNaVida.REDUTIVEL);
    }

    @Test
    void vermelhaEEvitavel() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.VERMELHA, Natureza.VARIAVEL))
                .contains(PapelNaVida.EVITAVEL);
    }

    @Test
    void verdeEDividaOuCompromisso() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.VERDE, Natureza.NAO_E_GASTO))
                .contains(PapelNaVida.DIVIDA_COMPROMISSO);
    }

    @Test
    void naoTriadaNaoTemPapelAinda() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.NAO_TRIADA, Natureza.VARIAVEL))
                .isEqualTo(Optional.empty());
    }

    @Test
    void azulComNaturezaInesperadaCaiParaEssencialPorSerIndispensavel() {
        assertThat(DerivadorDePapelNaVida.deriva(Etiqueta.AZUL, Natureza.NAO_E_GASTO))
                .contains(PapelNaVida.ESSENCIAL);
    }
}

package br.com.felipe.termometro.comparativo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculadoraDoComparativoTest {

    private static final Dinheiro RENDA = Dinheiro.de("10000");

    @Test
    void agrupaVariosItensNoMesmoGrupoESomaOPercentual() {
        Map<String, Dinheiro> itens = Map.of(
                "Aluguel", Dinheiro.de("2200"),
                "Energia (Energisa)", Dinheiro.de("300"),
                "Água", Dinheiro.de("55"),
                "Internet (Tely)", Dinheiro.de("129.90"));

        List<PontoComparativo> pontos = CalculadoraDoComparativo.calcula(itens, RENDA);

        PontoComparativo moradia = umico(pontos, GrupoDoComparativo.MORADIA);
        assertThat(moradia.atual().paraJson()).isEqualTo("0.268490");
    }

    @Test
    void grupoCalibradoTemBomIdealELimiteRuim() {
        List<PontoComparativo> pontos = CalculadoraDoComparativo.calcula(
                Map.of("Aluguel", Dinheiro.de("2200")), RENDA);

        PontoComparativo moradia = umico(pontos, GrupoDoComparativo.MORADIA);
        assertThat(moradia.bom()).isNotNull();
        assertThat(moradia.ideal()).isNotNull();
        assertThat(moradia.ruim()).isNotNull();
        assertThat(moradia.ruim().paraJson()).isEqualTo("0.312500");
    }

    @Test
    void grupoSemCalibracaoNaoTemBomNemIdealFabricado() {
        List<PontoComparativo> pontos = CalculadoraDoComparativo.calcula(
                Map.of("Transporte por app", Dinheiro.de("150")), RENDA);

        PontoComparativo transporte = umico(pontos, GrupoDoComparativo.TRANSPORTE);
        assertThat(transporte.bom()).isNull();
        assertThat(transporte.ideal()).isNull();
        assertThat(transporte.ruim()).isNull();
    }

    @Test
    void nomeDesconhecidoCaiEmOutrosSemSerDescartado() {
        List<PontoComparativo> pontos = CalculadoraDoComparativo.calcula(
                Map.of("Um item novo que ninguém mapeou ainda", Dinheiro.de("50")), RENDA);

        assertThat(pontos).hasSize(1);
        assertThat(pontos.get(0).grupo()).isEqualTo(GrupoDoComparativo.OUTROS);
    }

    @Test
    void semRendaDevolveListaVaziaEmVezDeDividirPorZero() {
        List<PontoComparativo> pontos = CalculadoraDoComparativo.calcula(
                Map.of("Aluguel", Dinheiro.de("2200")), Dinheiro.ZERO);

        assertThat(pontos).isEmpty();
    }

    private PontoComparativo umico(List<PontoComparativo> pontos, GrupoDoComparativo grupo) {
        return pontos.stream().filter(p -> p.grupo() == grupo).findFirst()
                .orElseThrow(() -> new AssertionError("grupo não encontrado: " + grupo));
    }
}

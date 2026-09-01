package br.com.felipe.termometro.reserva.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalculadoraDeNiveisDeReserva — RN-21")
class CalculadoraDeNiveisDeReservaTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final Competencia OUTUBRO = SETEMBRO.mais(1);
    private static final Competencia NOVEMBRO = SETEMBRO.mais(2);
    private static final Competencia DEZEMBRO = SETEMBRO.mais(3);
    private static final Competencia JANEIRO = SETEMBRO.mais(4);
    private static final Competencia FEVEREIRO = SETEMBRO.mais(5);

    /** Custo mensal de R$ 4.000 → alvos de R$ 4.000 / R$ 12.000 / R$ 24.000. */
    private static final Dinheiro CUSTO_MENSAL = Dinheiro.de("4000.00");

    @Test
    @DisplayName("cada nível marca a primeira competência em que a reserva projetada cruza o alvo")
    void cruzamentoDeNiveis() {
        List<MesProjetado> meses = List.of(
                mes(SETEMBRO, "1000.00"),
                mes(OUTUBRO, "4500.00"),
                mes(NOVEMBRO, "9000.00"),
                mes(DEZEMBRO, "12500.00"),
                mes(JANEIRO, "20000.00"),
                mes(FEVEREIRO, "25000.00"));

        PainelDeReserva painel = CalculadoraDeNiveisDeReserva.calcular(CUSTO_MENSAL, meses);

        assertThat(painel.custoMensal()).isEqualTo(CUSTO_MENSAL);
        assertThat(painel.niveis()).hasSize(3);

        StatusDoNivel umMes = status(painel, NivelDeReserva.UM_MES);
        assertThat(umMes.alvo()).isEqualTo(Dinheiro.de("4000.00"));
        assertThat(umMes.competenciaPrevista()).isEqualTo(OUTUBRO);
        // hoje é SETEMBRO — o cruzamento só acontece em OUTUBRO, então ainda não está "atingido".
        assertThat(umMes.atingido()).isFalse();

        StatusDoNivel tresMeses = status(painel, NivelDeReserva.TRES_MESES);
        assertThat(tresMeses.alvo()).isEqualTo(Dinheiro.de("12000.00"));
        assertThat(tresMeses.competenciaPrevista()).isEqualTo(DEZEMBRO);
        assertThat(tresMeses.atingido()).isFalse();

        StatusDoNivel seisMeses = status(painel, NivelDeReserva.SEIS_MESES);
        assertThat(seisMeses.alvo()).isEqualTo(Dinheiro.de("24000.00"));
        assertThat(seisMeses.competenciaPrevista()).isEqualTo(FEVEREIRO);
        assertThat(seisMeses.atingido()).isFalse();

        assertThat(painel.proximoNivel()).isEqualTo(NivelDeReserva.UM_MES);
    }

    @Test
    @DisplayName("quando o primeiro mês simulado já cruza o alvo, o nível vem atingido")
    void atingidoNoPrimeiroMes() {
        List<MesProjetado> meses = List.of(
                mes(SETEMBRO, "30000.00"),
                mes(OUTUBRO, "31000.00"));

        PainelDeReserva painel = CalculadoraDeNiveisDeReserva.calcular(CUSTO_MENSAL, meses);

        assertThat(painel.niveis()).allSatisfy(nivel -> {
            assertThat(nivel.atingido()).isTrue();
            assertThat(nivel.competenciaPrevista()).isEqualTo(SETEMBRO);
        });
        assertThat(painel.proximoNivel()).isNull();
    }

    @Test
    @DisplayName("nível não alcançado dentro do horizonte simulado vem sem competência prevista")
    void nivelForaDoHorizonte() {
        List<MesProjetado> meses = List.of(
                mes(SETEMBRO, "1000.00"),
                mes(OUTUBRO, "4500.00"),
                mes(NOVEMBRO, "9000.00"));

        PainelDeReserva painel = CalculadoraDeNiveisDeReserva.calcular(CUSTO_MENSAL, meses);

        StatusDoNivel tresMeses = status(painel, NivelDeReserva.TRES_MESES);
        assertThat(tresMeses.competenciaPrevista()).isNull();
        assertThat(tresMeses.atingido()).isFalse();

        StatusDoNivel seisMeses = status(painel, NivelDeReserva.SEIS_MESES);
        assertThat(seisMeses.competenciaPrevista()).isNull();
        assertThat(seisMeses.atingido()).isFalse();

        assertThat(painel.proximoNivel()).isEqualTo(NivelDeReserva.UM_MES);
    }

    @Test
    @DisplayName("custo mensal não positivo é erro")
    void custoMensalInvalido() {
        List<MesProjetado> meses = List.of(mes(SETEMBRO, "0.00"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalculadoraDeNiveisDeReserva.calcular(Dinheiro.ZERO, meses));
    }

    @Test
    @DisplayName("lista de meses vazia é erro")
    void mesesVazioEhErro() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalculadoraDeNiveisDeReserva.calcular(CUSTO_MENSAL, List.of()));
    }

    private StatusDoNivel status(PainelDeReserva painel, NivelDeReserva nivel) {
        return painel.niveis().stream().filter(s -> s.nivel() == nivel).findFirst().orElseThrow();
    }

    private static MesProjetado mes(Competencia competencia, String reservaAcumulada) {
        return new MesProjetado(competencia, Dinheiro.de("10000.00"), Dinheiro.de("4000.00"),
                Dinheiro.de("700.00"), Dinheiro.de("5300.00"), Dinheiro.ZERO, Dinheiro.ZERO,
                Dinheiro.de(reservaAcumulada), Dinheiro.ZERO, false);
    }
}

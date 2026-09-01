package br.com.felipe.termometro.reserva.domain;

import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RN-21 — varre a simulação de {@code meses} e encontra, para cada {@link NivelDeReserva}, a
 * primeira competência em que a reserva acumulada projetada atinge o alvo (custo mensal ×
 * multiplicador do nível).
 *
 * <p><b>Reserva atual = R$ 0 (decisão explícita de Felipe):</b> este código não tem, em lugar
 * nenhum, o valor já acumulado por Felipe fora do sistema — o sync do Pluggy não persiste saldo
 * de conta. Em vez de criar um campo manual novo no catálogo só para isso, a simulação de
 * {@code meses} já parte de zero (é como {@link MesProjetado#reservaAcumulada()} sempre
 * funciona), e é reaproveitada tal como está. Consequência aceita e documentada: como toda nova
 * consulta projeta de novo a partir de zero na competência corrente, {@code atingido} praticamente
 * nunca será {@code true} numa chamada feita depois do primeiro mês simulado — só é {@code true}
 * quando o próprio primeiro mês da simulação já cruza o alvo.
 */
public final class CalculadoraDeNiveisDeReserva {

    private CalculadoraDeNiveisDeReserva() {
    }

    public static PainelDeReserva calcular(Dinheiro custoMensal, List<MesProjetado> meses) {
        Objects.requireNonNull(custoMensal, "custo mensal não pode ser nulo");
        Objects.requireNonNull(meses, "meses não podem ser nulos");
        if (!custoMensal.ehPositivo()) {
            throw new IllegalArgumentException("custo mensal deve ser positivo: " + custoMensal);
        }
        if (meses.isEmpty()) {
            throw new IllegalArgumentException("a simulação precisa de ao menos um mês");
        }

        Competencia primeiraCompetencia = meses.get(0).competencia();
        List<StatusDoNivel> niveis = new ArrayList<>();
        for (NivelDeReserva nivel : NivelDeReserva.values()) {
            Dinheiro alvo = custoMensal.multiplicar(nivel.multiplicador());
            Competencia competenciaPrevista = meses.stream()
                    .filter(mes -> mes.reservaAcumulada().maiorOuIgualA(alvo))
                    .map(MesProjetado::competencia)
                    .findFirst()
                    .orElse(null);
            boolean atingido = primeiraCompetencia.equals(competenciaPrevista);
            niveis.add(new StatusDoNivel(nivel, alvo, atingido, competenciaPrevista));
        }

        NivelDeReserva proximoNivel = niveis.stream()
                .filter(status -> !status.atingido())
                .map(StatusDoNivel::nivel)
                .findFirst()
                .orElse(null);

        return new PainelDeReserva(custoMensal, niveis, proximoNivel);
    }
}

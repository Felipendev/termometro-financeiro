package br.com.felipe.termometro.projecao.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * RN-09 — resultado completo de uma simulação: o mês a mês, os marcos extraídos dele, o
 * veredito e, quando {@code status == INVIAVEL}, a renda extra mínima sugerida.
 *
 * @param meses um {@link MesProjetado} por competência simulada, na ordem, começando em
 *              {@code competenciaInicio}
 * @param rendaExtraMinimaSugerida {@code null} sempre que {@code status != INVIAVEL}; nunca
 *                                 {@code null} quando é
 */
public record Projecao(
        Competencia competenciaInicio,
        Estrategia estrategia,
        List<MesProjetado> meses,
        Marcos marcos,
        StatusProjecao status,
        @Nullable Dinheiro rendaExtraMinimaSugerida) {

    public Projecao {
        Objects.requireNonNull(competenciaInicio, "competência inicial não pode ser nula");
        Objects.requireNonNull(estrategia, "estratégia não pode ser nula");
        Objects.requireNonNull(meses, "meses não podem ser nulos");
        Objects.requireNonNull(marcos, "marcos não podem ser nulos");
        Objects.requireNonNull(status, "status não pode ser nulo");
        if (meses.isEmpty()) {
            throw new IllegalArgumentException("a simulação precisa de ao menos um mês");
        }
        meses = List.copyOf(meses);
        boolean inviavel = status == StatusProjecao.INVIAVEL;
        if (inviavel && rendaExtraMinimaSugerida == null) {
            throw new IllegalArgumentException(
                    "status INVIAVEL precisa vir com renda extra mínima sugerida");
        }
        if (!inviavel && rendaExtraMinimaSugerida != null) {
            throw new IllegalArgumentException(
                    "renda extra mínima sugerida só faz sentido com status INVIAVEL");
        }
    }
}

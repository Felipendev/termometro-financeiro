package br.com.felipe.termometro.planoajuste.domain;

import java.util.List;
import java.util.Objects;

/**
 * Saída de {@link CalculadoraDeRampa}: quantos meses a rampa efetivamente levou (pode ser mais
 * que o solicitado, se o pedido era curto demais para o limite de corte) e os alvos mês a mês.
 *
 * @param alongada {@code true} quando {@code mesesEfetivos} é maior que o solicitado pelo
 *                 chamador — sinal para o motor emitir o aviso que a RN-15 exige
 */
public record ResultadoDaRampa(int mesesEfetivos, boolean alongada, List<AlvoMensal> alvosMensais) {

    public ResultadoDaRampa {
        Objects.requireNonNull(alvosMensais, "alvosMensais não pode ser nulo");
        alvosMensais = List.copyOf(alvosMensais);
        if (alvosMensais.isEmpty()) {
            throw new IllegalArgumentException("alvosMensais não pode ser vazio");
        }
        if (mesesEfetivos != alvosMensais.size()) {
            throw new IllegalArgumentException(
                    "mesesEfetivos (%d) precisa bater com o tamanho de alvosMensais (%d)"
                            .formatted(mesesEfetivos, alvosMensais.size()));
        }
    }
}

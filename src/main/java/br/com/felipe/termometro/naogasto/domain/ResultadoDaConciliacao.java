package br.com.felipe.termometro.naogasto.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Saída do {@link MotorDeNaoGasto}: quais lançamentos viram {@code ignorada = true}, quanto isso
 * representa, e detalhes legíveis por tipo de casamento — para o resumo do
 * {@code POST /v1/nao-gasto/{competencia}}.
 */
public record ResultadoDaConciliacao(
        Set<UUID> idsParaIgnorar, int pagamentosDeFaturaCasados, int transferenciasCasadas,
        int estornosCasados, Dinheiro valorTotalIgnorado, List<String> detalhes) {

    public ResultadoDaConciliacao {
        Objects.requireNonNull(idsParaIgnorar, "idsParaIgnorar não pode ser nulo");
        Objects.requireNonNull(valorTotalIgnorado, "valorTotalIgnorado não pode ser nulo");
        Objects.requireNonNull(detalhes, "detalhes não pode ser nulo");
        idsParaIgnorar = Set.copyOf(idsParaIgnorar);
        detalhes = List.copyOf(detalhes);
    }
}

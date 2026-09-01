package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.shared.Dinheiro;

/**
 * Projeção enxuta de {@link CompromissoFuturo} pro dashboard — sem os campos internos de série
 * ({@code identificadorConta}, {@code parcelaNumero}/{@code parcelaTotal}, {@code confirmado})
 * que não interessam pra UI da coluna "Eu do Passado".
 */
public record CompromissoFuturoItemResponse(String descricao, String competencia, Dinheiro valor) {

    public CompromissoFuturoItemResponse(CompromissoFuturo compromisso) {
        this(compromisso.descricao(), compromisso.competencia().toString(), compromisso.valor());
    }
}

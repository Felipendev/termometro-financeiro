package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.planoajuste.application.api.response.PlanoDeAjusteResponse;
import br.com.felipe.termometro.projecao.application.api.response.MarcosResponse;
import br.com.felipe.termometro.reserva.application.api.response.PainelDeReservaResponse;
import org.jspecify.annotations.Nullable;

/**
 * Marcos da projeção de quitação (RN-09), painel de reserva em níveis (RN-21) e plano de ajuste
 * progressivo (RN-15). O simulador de compra (RN-11, único jeito formal de entrada de novas
 * parcelas segundo a spec) fica fora desta fatia — ver Javadoc de {@code DashboardAPI}.
 */
public record EuDoFuturoResponse(
        MarcosResponse marcos,
        @Nullable PainelDeReservaResponse reserva,
        @Nullable String reservaIndisponivel,
        PlanoDeAjusteResponse planoAjuste) {
}

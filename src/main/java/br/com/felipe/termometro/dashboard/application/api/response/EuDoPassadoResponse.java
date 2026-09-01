package br.com.felipe.termometro.dashboard.application.api.response;

import br.com.felipe.termometro.catalogo.application.api.response.DividaResponse;
import java.util.List;

/** "O que eu já assinei e ainda vou pagar": compromissos futuros de compra parcelada + dívidas ativas. */
public record EuDoPassadoResponse(
        List<CompromissoFuturoItemResponse> compromissosFuturos,
        List<DividaResponse> dividas) {
}

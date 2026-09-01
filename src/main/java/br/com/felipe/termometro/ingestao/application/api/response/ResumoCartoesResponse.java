package br.com.felipe.termometro.ingestao.application.api.response;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;

public record ResumoCartoesResponse(List<CartaoResponse> cartoes, Dinheiro totalGastoEmCartoes) {

    public static ResumoCartoesResponse de(List<CartaoResponse> cartoes) {
        Dinheiro total = Dinheiro.somaDe(cartoes.stream().map(CartaoResponse::gastoNoMes).toList());
        return new ResumoCartoesResponse(cartoes, total);
    }
}

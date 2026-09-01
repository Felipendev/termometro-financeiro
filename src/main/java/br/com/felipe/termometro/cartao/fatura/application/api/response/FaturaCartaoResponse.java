package br.com.felipe.termometro.cartao.fatura.application.api.response;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record FaturaCartaoResponse(
        String referencia,
        String nome,
        @Nullable Dinheiro limite,
        Dinheiro valorTotal,
        Dinheiro valorPago,
        Dinheiro saldoAberto,
        String status,
        String origem,
        List<PagamentoResponse> pagamentos) {

    public record PagamentoResponse(String id, Dinheiro valor, LocalDate data, String lancamentoId) { }
}

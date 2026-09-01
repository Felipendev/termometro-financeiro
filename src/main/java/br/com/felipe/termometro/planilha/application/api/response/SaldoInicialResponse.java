package br.com.felipe.termometro.planilha.application.api.response;

import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import java.time.LocalDate;

public record SaldoInicialResponse(LocalDate dataReferencia, String valor) {

    public static SaldoInicialResponse de(SaldoInicialPlanilha saldo) {
        return new SaldoInicialResponse(saldo.dataReferencia(), saldo.valor().paraJson());
    }
}

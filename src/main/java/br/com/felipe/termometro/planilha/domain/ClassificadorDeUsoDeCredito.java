package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * RN-18 — dois sinais, cada um independente do outro:
 *
 * <p><b>Sinal 1 — Pix no crédito.</b> Regra do Felipe: "crédito nunca deve ser tratado como
 * renda". Pix financiado pelo limite do cartão é, quase sempre, dinheiro que deveria estar
 * disponível na hora.
 *
 * <p><b>Sinal 2 — caixa do dia já negativo.</b> O saldo em cascata imediatamente antes desta
 * transação específica ser descontada — não o saldo do mês, o do dia, no ponto exato em que ela
 * aconteceu.
 */
public final class ClassificadorDeUsoDeCredito {

    private ClassificadorDeUsoDeCredito() {
    }

    public static UsoDeCredito classifica(String descricaoDoItem, Dinheiro saldoAntesDoItem) {
        Objects.requireNonNull(descricaoDoItem, "descrição não pode ser nula");
        Objects.requireNonNull(saldoAntesDoItem, "saldo antes do item não pode ser nulo");

        boolean pixNoCredito = descricaoDoItem.toUpperCase().contains("PIX");
        boolean caixaDoDiaNegativo = saldoAntesDoItem.ehNegativo();

        if (pixNoCredito && caixaDoDiaNegativo) {
            return UsoDeCredito.DEFICIT_DISFARCADO;
        }
        if (pixNoCredito || caixaDoDiaNegativo) {
            return UsoDeCredito.ATENCAO;
        }
        return UsoDeCredito.FERRAMENTA;
    }
}

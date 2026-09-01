package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;

/**
 * O semáforo do saldo, dia a dia. Limiares fixos por ora — quando houver mais histórico real
 * fluindo pela planilha, calibrar contra o custo mensal do Felipe em vez de valores redondos.
 */
public enum FaixaDeSaldo {
    VERMELHO,
    LARANJA,
    AMARELO,
    VERDE_CLARO,
    VERDE;

    private static final Dinheiro LIMITE_LARANJA = Dinheiro.de("1500");
    private static final Dinheiro LIMITE_AMARELO = Dinheiro.de("4000");
    private static final Dinheiro LIMITE_VERDE_CLARO = Dinheiro.de("7000");

    public static FaixaDeSaldo de(Dinheiro saldo) {
        if (saldo.ehNegativo()) {
            return VERMELHO;
        }
        if (saldo.menorQue(LIMITE_LARANJA)) {
            return LARANJA;
        }
        if (saldo.menorQue(LIMITE_AMARELO)) {
            return AMARELO;
        }
        if (saldo.menorQue(LIMITE_VERDE_CLARO)) {
            return VERDE_CLARO;
        }
        return VERDE;
    }
}

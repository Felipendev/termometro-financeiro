package br.com.felipe.termometro.reserva.domain;

/**
 * RN-21 — os três níveis com alvo fixo em múltiplos de {@code custoMensal}. O nível 0 (ponte de
 * caixa) da spec fica de fora: depende de RN-08.1 (maior compromisso que vence antes da renda),
 * a mesma "ponte de caixa" já deixada fora do escopo desde a fatia 4 (RN-08) por falta de dado
 * real fluindo pelo Pluggy o bastante para calculá-la com confiança.
 */
public enum NivelDeReserva {

    UM_MES(1),
    TRES_MESES(3),
    SEIS_MESES(6);

    private final int multiplicador;

    NivelDeReserva(int multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int multiplicador() {
        return multiplicador;
    }
}

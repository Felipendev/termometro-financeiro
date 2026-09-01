package br.com.felipe.termometro.ingestao.domain;

/**
 * De onde a transação veio. A ordem do enum é a ordem de confiança: quando a mesma transação
 * chega por duas fontes (RN-02), vence a de maior confiança.
 */
public enum Origem {
    MANUAL,
    PDF,
    CSV,
    OFX;

    public boolean maisConfiavelQue(Origem outra) {
        return ordinal() > outra.ordinal();
    }
}

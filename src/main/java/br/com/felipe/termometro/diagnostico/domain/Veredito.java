package br.com.felipe.termometro.diagnostico.domain;

/** O resultado da RN-16, em uma palavra. */
public enum Veredito {

    /** {@code TaxaMaxima >= meta}: dá, sem mexer no padrão. O gap está no amarelo/vermelho — é execução. */
    VIAVEL,

    /** {@code 0 < TaxaMaxima < meta}: no melhor cenário possível, sobra menos que a meta. */
    VIAVEL_PARCIALMENTE,

    /** {@code TaxaMaxima <= 0}: o custo mínimo de vida excede a renda. Nenhuma disciplina resolve. */
    INVIAVEL
}

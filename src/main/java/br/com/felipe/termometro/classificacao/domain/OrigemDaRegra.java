package br.com.felipe.termometro.classificacao.domain;

import java.math.BigDecimal;

/**
 * Quem criou a regra. A ordem do enum é a ordem de precedência: <b>a decisão do usuário sempre
 * vence a do sistema</b>, e uma correção manual nunca é desfeita por uma regra padrão.
 */
public enum OrigemDaRegra {

    /**
     * Catálogo embutido, calibrado nas faturas reais.
     *
     * <p>0,85 não é chute. Combinado com o multiplicador de {@link TipoDeRegra}, produz
     * exatamente a separação que se quer contra o limiar de 0,70 da {@link Classificacao}:
     *
     * <ul>
     *   <li>CNPJ 0,85 · estabelecimento exato 0,81 · descrição <b>0,72</b> — decidem sozinhos;</li>
     *   <li>categoria do banco <b>0,68</b> — <i>não</i> decide sozinha, vai para revisão.</li>
     * </ul>
     *
     * A margem apertada da regra por descrição é intencional: ela acerta na maioria e erra o
     * suficiente para não merecer mais que isso. Uma dica do banco, sozinha, merece menos ainda.
     */
    SISTEMA(new BigDecimal("0.85")),

    /** Derivada de uma classificação manual anterior do mesmo estabelecimento (RN-12). */
    APRENDIZADO(new BigDecimal("0.90")),

    /** Escrita pelo usuário. Confiança total: ele sabe o que comprou. */
    USUARIO(BigDecimal.ONE);

    private final BigDecimal confiancaBase;

    OrigemDaRegra(BigDecimal confiancaBase) {
        this.confiancaBase = confiancaBase;
    }

    public BigDecimal confiancaBase() {
        return confiancaBase;
    }

    public boolean vencePrecedencia(OrigemDaRegra outra) {
        return ordinal() > outra.ordinal();
    }
}

package br.com.felipe.termometro.classificacao.domain;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * O resultado de classificar uma transação.
 *
 * @param contaNoDiaADia se este gasto entra na verba diária (RN-19) — o campo que o orçamento lê
 * @param precisaRevisao confiança abaixo do limiar: vai para a fila da RN-12 em vez de ser
 *                       aplicada em silêncio
 */
public record Classificacao(
        Categoria categoria,
        BigDecimal confianca,
        @Nullable OrigemDaRegra origem,
        boolean contaNoDiaADia,
        boolean precisaRevisao,
        String motivo) {

    /**
     * Abaixo disto o sistema não decide sozinho (RN-05).
     *
     * <p>Calibrado junto com {@link OrigemDaRegra} e {@link TipoDeRegra}: regra do catálogo por
     * descrição fica em 0,72 e passa; dica de categoria do banco fica em 0,68 e vai para revisão.
     */
    public static final BigDecimal LIMIAR_DE_CONFIANCA = new BigDecimal("0.70");

    public Classificacao {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(confianca, "confiança não pode ser nula");
        Objects.requireNonNull(motivo, "motivo não pode ser nulo");
    }

    public boolean confiavel() {
        return confianca.compareTo(LIMIAR_DE_CONFIANCA) >= 0;
    }
}

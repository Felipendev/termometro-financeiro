package br.com.felipe.termometro.ingestao.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * Confere a soma do que foi lido contra o total impresso na fatura.
 *
 * <p>Esta é a rede de segurança de todo o sistema. Um parser que perde lançamentos não falha —
 * ele produz um número menor e plausível, e o diagnóstico inteiro passa a mentir para baixo.
 * Sem esta conferência, a primeira leitura da fatura de julho teria entrado no banco com
 * R$ 4.465,28 no lugar dos R$ 4.091,57 impressos, e ninguém saberia.
 */
public record Reconciliacao(Dinheiro totalLido, Dinheiro totalDeclarado, Dinheiro tolerancia) {

    /** Tolerância padrão: um centavo. Fatura fecha no centavo ou não fecha. */
    public static final Dinheiro TOLERANCIA_PADRAO = Dinheiro.de("0.01");

    public Reconciliacao {
        Objects.requireNonNull(totalLido, "total lido não pode ser nulo");
        Objects.requireNonNull(totalDeclarado, "total declarado não pode ser nulo");
        Objects.requireNonNull(tolerancia, "tolerância não pode ser nula");
        if (tolerancia.ehNegativo()) {
            throw new IllegalArgumentException("tolerância não pode ser negativa: " + tolerancia);
        }
    }

    public static Reconciliacao de(Dinheiro totalLido, Dinheiro totalDeclarado) {
        return new Reconciliacao(totalLido, totalDeclarado, TOLERANCIA_PADRAO);
    }

    public Dinheiro diferenca() {
        return totalLido.subtrair(totalDeclarado);
    }

    public boolean fecha() {
        return diferenca().absoluto().menorOuIgualA(tolerancia);
    }

    public String relatorio() {
        if (fecha()) {
            return "fatura fecha em " + totalDeclarado;
        }
        return "fatura NÃO fecha: lido " + totalLido + ", declarado " + totalDeclarado
                + ", diferença " + diferenca();
    }
}

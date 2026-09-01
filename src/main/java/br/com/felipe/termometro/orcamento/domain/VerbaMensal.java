package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * Teto de gasto variável do mês, dividido em duas camadas (RN-20).
 *
 * <p>A <b>provisão fica dentro da verba, não em cima</b>. Somar um colchão ao orçamento é a forma
 * mais comum de fazer um orçamento mentir: o número final vira o que se queria ver, não o que se
 * pode gastar. Uma verba de R$ 3.000 com R$ 250 de provisão dá R$ 2.750 de dia a dia — nunca
 * R$ 3.250 de teto.
 *
 * @param competencia   mês de referência
 * @param verbaVariavel teto total do mês
 * @param provisao      camada reservada para eventos e imprevistos, contida na verba
 */
public record VerbaMensal(Competencia competencia, Dinheiro verbaVariavel, Dinheiro provisao) {

    public VerbaMensal {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(verbaVariavel, "verba não pode ser nula");
        Objects.requireNonNull(provisao, "provisão não pode ser nula");
        if (verbaVariavel.ehNegativo()) {
            throw new IllegalArgumentException("verba não pode ser negativa: " + verbaVariavel);
        }
        if (provisao.ehNegativo()) {
            throw new IllegalArgumentException("provisão não pode ser negativa: " + provisao);
        }
        if (provisao.maiorQue(verbaVariavel)) {
            throw new IllegalArgumentException(
                    "a provisão fica dentro da verba: provisão " + provisao + " > verba " + verbaVariavel);
        }
    }

    /** O que sobra para o cotidiano, depois de separar a provisão. */
    public Dinheiro diaADia() {
        return verbaVariavel.subtrair(provisao);
    }

    /** Verba de um dia médio, se o mês fosse perfeitamente uniforme. É a régua do semáforo. */
    public Dinheiro verbaBase() {
        return diaADia().dividirPor(java.math.BigDecimal.valueOf(competencia.quantidadeDeDias()));
    }
}

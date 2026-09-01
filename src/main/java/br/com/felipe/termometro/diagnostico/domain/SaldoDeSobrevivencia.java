package br.com.felipe.termometro.diagnostico.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * RN-08 — o que sobra depois do que já está comprometido: fixo, piso do variável e serviço da
 * dívida. É uma foto de um mês só, diferente da projeção (RN-09, ainda não implementada) que
 * simula a dívida em aberto sendo paga mês a mês — os dois números não precisam bater.
 *
 * @param rendaExtraNecessaria {@link Dinheiro#ZERO} quando não há déficit; nunca {@code null}
 */
public record SaldoDeSobrevivencia(
        Competencia competencia,
        Dinheiro rendaLiquida,
        Dinheiro comprometidoFixo,
        Dinheiro minimoVariavel,
        Dinheiro servicoDivida,
        Dinheiro totalComprometido,
        Dinheiro saldo,
        boolean deficit,
        Dinheiro rendaExtraNecessaria) {

    public SaldoDeSobrevivencia {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(comprometidoFixo, "comprometido fixo não pode ser nulo");
        Objects.requireNonNull(minimoVariavel, "mínimo variável não pode ser nulo");
        Objects.requireNonNull(servicoDivida, "serviço da dívida não pode ser nulo");
        Objects.requireNonNull(totalComprometido, "total comprometido não pode ser nulo");
        Objects.requireNonNull(saldo, "saldo não pode ser nulo");
        Objects.requireNonNull(rendaExtraNecessaria, "renda extra necessária não pode ser nula");
    }
}

package br.com.felipe.termometro.diagnostico.application.api.response;

import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.shared.Dinheiro;

/** RN-08 — a foto de um mês só: o que sobra depois do que já está comprometido. */
public record SaldoDeSobrevivenciaResponse(
        String competencia,
        Dinheiro rendaLiquida,
        Dinheiro comprometidoFixo,
        Dinheiro minimoVariavel,
        Dinheiro servicoDivida,
        Dinheiro totalComprometido,
        Dinheiro saldo,
        boolean deficit,
        Dinheiro rendaExtraNecessaria) {

    public SaldoDeSobrevivenciaResponse(SaldoDeSobrevivencia saldo) {
        this(saldo.competencia().toString(), saldo.rendaLiquida(), saldo.comprometidoFixo(),
                saldo.minimoVariavel(), saldo.servicoDivida(), saldo.totalComprometido(), saldo.saldo(),
                saldo.deficit(), saldo.rendaExtraNecessaria());
    }
}

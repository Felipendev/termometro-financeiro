package br.com.felipe.termometro.projecao.application.api.response;

import br.com.felipe.termometro.projecao.domain.Marcos;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;

public record MarcosResponse(
        String dataQuitacao,
        String primeiroRealGuardado,
        String reservaCompleta,
        Dinheiro jurosTotaisPagos,
        Integer mesesAteQuitacao) {

    public MarcosResponse(Marcos marcos) {
        this(paraTexto(marcos.dataQuitacao()), paraTexto(marcos.primeiroRealGuardado()),
                paraTexto(marcos.reservaCompleta()), marcos.jurosTotaisPagos(), marcos.mesesAteQuitacao());
    }

    private static String paraTexto(Competencia competencia) {
        return competencia == null ? null : competencia.toString();
    }
}

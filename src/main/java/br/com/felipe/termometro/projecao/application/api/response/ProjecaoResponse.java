package br.com.felipe.termometro.projecao.application.api.response;

import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.projecao.domain.StatusProjecao;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;

public record ProjecaoResponse(
        String competenciaInicio,
        Estrategia estrategia,
        List<MesProjetadoResponse> meses,
        MarcosResponse marcos,
        StatusProjecao status,
        Dinheiro rendaExtraMinimaSugerida) {

    public ProjecaoResponse(Projecao projecao) {
        this(projecao.competenciaInicio().toString(), projecao.estrategia(),
                projecao.meses().stream().map(MesProjetadoResponse::new).toList(),
                new MarcosResponse(projecao.marcos()), projecao.status(), projecao.rendaExtraMinimaSugerida());
    }
}

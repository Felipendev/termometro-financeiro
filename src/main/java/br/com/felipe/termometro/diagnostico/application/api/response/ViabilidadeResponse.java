package br.com.felipe.termometro.diagnostico.application.api.response;

import br.com.felipe.termometro.diagnostico.domain.Veredito;
import br.com.felipe.termometro.diagnostico.domain.Viabilidade;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import org.jspecify.annotations.Nullable;

/** RN-16 — a pergunta central, traduzida para o cliente. */
public record ViabilidadeResponse(
        String competencia,
        Dinheiro rendaLiquida,
        Dinheiro custoFixoTotal,
        Dinheiro pisoVariavelTotal,
        Dinheiro custoMinimoVida,
        Dinheiro economiaMaxima,
        Percentual taxaMaxima,
        Percentual metaEconomia,
        Veredito veredito,
        Dinheiro alvoReducaoFixo,
        @Nullable QuedaDeRendaResponse quedaDeRenda,
        String leitura) {

    public ViabilidadeResponse(Viabilidade viabilidade) {
        this(viabilidade.competencia().toString(), viabilidade.rendaLiquida(),
                viabilidade.custoFixoTotal(), viabilidade.pisoVariavelTotal(),
                viabilidade.custoMinimoVida(), viabilidade.economiaMaxima(), viabilidade.taxaMaxima(),
                viabilidade.metaEconomia(), viabilidade.veredito(), viabilidade.alvoReducaoFixo(),
                viabilidade.quedaDeRenda() == null ? null : new QuedaDeRendaResponse(viabilidade.quedaDeRenda()),
                viabilidade.leitura());
    }
}

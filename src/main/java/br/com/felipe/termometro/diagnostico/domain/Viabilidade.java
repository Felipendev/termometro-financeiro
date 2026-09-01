package br.com.felipe.termometro.diagnostico.domain;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Resposta da RN-16 — a pergunta central: dá para guardar {@code metaEconomia} da renda com o
 * padrão de vida atual, ou o padrão precisa cair?
 *
 * @param quedaDeRenda {@code null} quando a queda estrutural (RN-16.1) não foi detectada — seja
 *                     porque não houve, seja porque ainda não há 6 meses de histórico de renda
 */
public record Viabilidade(
        Competencia competencia,
        Dinheiro rendaLiquida,
        Dinheiro custoFixoTotal,
        Dinheiro pisoVariavelTotal,
        Dinheiro custoMinimoVida,
        Dinheiro economiaMaxima,
        Percentual taxaMaxima,
        Percentual metaEconomia,
        Veredito veredito,
        Dinheiro alvoReducaoFixo,
        @Nullable QuedaDeRenda quedaDeRenda,
        String leitura) {

    public Viabilidade {
        Objects.requireNonNull(competencia, "competência não pode ser nula");
        Objects.requireNonNull(rendaLiquida, "renda líquida não pode ser nula");
        Objects.requireNonNull(custoFixoTotal, "custo fixo total não pode ser nulo");
        Objects.requireNonNull(pisoVariavelTotal, "piso variável total não pode ser nulo");
        Objects.requireNonNull(custoMinimoVida, "custo mínimo de vida não pode ser nulo");
        Objects.requireNonNull(economiaMaxima, "economia máxima não pode ser nula");
        Objects.requireNonNull(taxaMaxima, "taxa máxima não pode ser nula");
        Objects.requireNonNull(metaEconomia, "meta de economia não pode ser nula");
        Objects.requireNonNull(veredito, "veredito não pode ser nulo");
        Objects.requireNonNull(alvoReducaoFixo, "alvo de redução de fixo não pode ser nulo");
        Objects.requireNonNull(leitura, "leitura não pode ser nula");
    }
}

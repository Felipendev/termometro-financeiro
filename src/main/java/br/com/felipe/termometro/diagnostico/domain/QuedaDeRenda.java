package br.com.felipe.termometro.diagnostico.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import java.util.Objects;

/**
 * RN-16.1 — só existe quando a queda estrutural foi detectada (mediana dos últimos 3 meses <
 * 85% da mediana dos 3 anteriores, com histórico de 6 meses). Aparece no topo do diagnóstico,
 * antes de qualquer sugestão de corte: o problema é aritmética, não disciplina.
 */
public record QuedaDeRenda(Dinheiro rendaAnterior, Dinheiro rendaAtual, Percentual quedaPct,
                            Percentual pesoFixoAntes, Percentual pesoFixoAgora,
                            Dinheiro excedenteEstrutural, String mensagem) {

    public QuedaDeRenda {
        Objects.requireNonNull(rendaAnterior, "renda anterior não pode ser nula");
        Objects.requireNonNull(rendaAtual, "renda atual não pode ser nula");
        Objects.requireNonNull(quedaPct, "queda percentual não pode ser nula");
        Objects.requireNonNull(pesoFixoAntes, "peso do fixo antes não pode ser nulo");
        Objects.requireNonNull(pesoFixoAgora, "peso do fixo agora não pode ser nulo");
        Objects.requireNonNull(excedenteEstrutural, "excedente estrutural não pode ser nulo");
        Objects.requireNonNull(mensagem, "mensagem não pode ser nula");
    }
}

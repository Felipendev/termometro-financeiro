package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;

/**
 * Totais de uma categoria no mês, por cor — a saída de {@code GET /v1/triagem/{competencia}/resumo}.
 * Recalculado sob demanda a cada leitura (como {@code viabilidade} e {@code diagnostico}), nunca
 * persistido: é o único jeito de manter a divisão da transação-fronteira (RN-05) sem duplicar
 * estado que poderia ficar velho se o piso mudar.
 */
public record ResumoDeCategoria(
        String categoria, Natureza natureza,
        Dinheiro totalAzul, Dinheiro totalAmarelo, Dinheiro totalVermelho, Dinheiro totalVerde,
        Dinheiro totalNaoTriada) {

    public ResumoDeCategoria {
        Objects.requireNonNull(categoria, "categoria não pode ser nula");
        Objects.requireNonNull(natureza, "natureza não pode ser nula");
        Objects.requireNonNull(totalAzul, "totalAzul não pode ser nulo");
        Objects.requireNonNull(totalAmarelo, "totalAmarelo não pode ser nulo");
        Objects.requireNonNull(totalVermelho, "totalVermelho não pode ser nulo");
        Objects.requireNonNull(totalVerde, "totalVerde não pode ser nulo");
        Objects.requireNonNull(totalNaoTriada, "totalNaoTriada não pode ser nulo");
    }
}

package br.com.felipe.termometro.triagem.domain;

import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Uma transação já classificada pela RN-12 (tem categoria e natureza), pronta para a triagem de
 * cores da RN-05.
 *
 * @param valor         valor absoluto da despesa — o sinal já foi resolvido na ingestão (RN-01) e
 *                      não importa para a triagem
 * @param etiquetaAtual o que já está persistido, quando houver. {@code VERMELHA} é a única
 *                      etiqueta que só chega aqui por decisão manual — o motor de triagem nunca a
 *                      atribui sozinho, e por isso preserva sempre que a encontra
 */
public record TransacaoClassificada(
        UUID id, LocalDate data, Dinheiro valor, String categoria, Natureza natureza,
        @Nullable Etiqueta etiquetaAtual) {

    public TransacaoClassificada {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(natureza, "natureza não pode ser nula");
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("categoria não pode ser vazia");
        }
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException(
                    "valor da transação classificada deve ser positivo: " + valor);
        }
    }

    public Optional<Etiqueta> etiquetaAtualOpcional() {
        return Optional.ofNullable(etiquetaAtual);
    }

    public boolean promovidaManualmenteParaVermelha() {
        return etiquetaAtual == Etiqueta.VERMELHA;
    }
}

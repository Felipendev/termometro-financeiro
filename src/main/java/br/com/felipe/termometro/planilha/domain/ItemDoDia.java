package br.com.felipe.termometro.planilha.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Um lançamento dentro da composição do dia — a soma que a célula mostra nunca é um número
 * solto, é sempre a soma desta lista (decisão tomada com o Felipe: cada valor discriminado,
 * nunca escondido atrás de um total).
 *
 * @param origem valor de {@code Origem} para itens importados (PDF/CSV/OFX) ou {@code "MANUAL"}
 *               para o que nasceu de um {@code LancamentoPlanejado}
 * @param usoDeCredito RN-18 — só preenchido para saídas de cartão (origem diferente de MANUAL);
 *                     {@code null} para entradas e para lançamentos manuais, que não fazem parte
 *                     dessa análise
 */
public record ItemDoDia(
        String descricao,
        Dinheiro valor,
        TipoItemDoDia tipo,
        String origem,
        @Nullable UsoDeCredito usoDeCredito,
        @Nullable UUID id,
        boolean editavel,
        @Nullable String categoria,
        @Nullable String grupoCategoria,
        @Nullable String naturezaCategoria,
        @Nullable String origemReceita) {

    public ItemDoDia(String descricao, Dinheiro valor, TipoItemDoDia tipo, String origem) {
        this(descricao, valor, tipo, origem, null, null, false, null, null, null, null);
    }

    public ItemDoDia(String descricao, Dinheiro valor, TipoItemDoDia tipo, String origem,
            @Nullable UsoDeCredito usoDeCredito) {
        this(descricao, valor, tipo, origem, usoDeCredito, null, false, null, null, null, null);
    }

    public ItemDoDia {
        Objects.requireNonNull(descricao, "descrição não pode ser nula");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(origem, "origem não pode ser nula");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor do item deve ser positivo, o sinal é o tipo");
        }
    }

    public ItemDoDia comUsoDeCredito(UsoDeCredito novoUsoDeCredito) {
        return new ItemDoDia(descricao, valor, tipo, origem, novoUsoDeCredito, id, editavel,
                categoria, grupoCategoria, naturezaCategoria, origemReceita);
    }

    public boolean vemDeCartao() {
        return !"MANUAL".equals(origem);
    }
}

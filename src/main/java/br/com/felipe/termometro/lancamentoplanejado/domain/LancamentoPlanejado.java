package br.com.felipe.termometro.lancamentoplanejado.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record LancamentoPlanejado(
        UUID id,
        String descricao,
        TipoLancamentoPlanejado tipo,
        Dinheiro valor,
        LocalDate vencimento,
        StatusLancamentoPlanejado status,
        UUID contaOrigemId,
        UUID contaDestinoId,
        CategoriaDoLancamento categoria,
        UUID cartaoManualId,
        UUID transacaoId,
        MarcacaoPlanejamento marcacaoPlanejamento,
        OrigemReceita origemReceita) {

    public LancamentoPlanejado(UUID id, String descricao, TipoLancamentoPlanejado tipo,
            Dinheiro valor, LocalDate vencimento, StatusLancamentoPlanejado status) {
        this(id, descricao, tipo, valor, vencimento, status, null, null, null, null, null,
                MarcacaoPlanejamento.NENHUMA, null);
    }

    public LancamentoPlanejado(UUID id, String descricao, TipoLancamentoPlanejado tipo,
            Dinheiro valor, LocalDate vencimento, StatusLancamentoPlanejado status,
            UUID contaOrigemId, UUID contaDestinoId) {
        this(id, descricao, tipo, valor, vencimento, status, contaOrigemId, contaDestinoId,
                null, null, null, MarcacaoPlanejamento.NENHUMA, null);
    }

    /** Compatibilidade para os chamadores anteriores à marcação de planejamento. */
    public LancamentoPlanejado(UUID id, String descricao, TipoLancamentoPlanejado tipo,
            Dinheiro valor, LocalDate vencimento, StatusLancamentoPlanejado status,
            UUID contaOrigemId, UUID contaDestinoId, CategoriaDoLancamento categoria,
            UUID cartaoManualId, UUID transacaoId) {
        this(id, descricao, tipo, valor, vencimento, status, contaOrigemId, contaDestinoId,
                categoria, cartaoManualId, transacaoId, MarcacaoPlanejamento.NENHUMA, null);
    }

    /** Compatibilidade para registros e chamadores anteriores à origem própria da receita. */
    public LancamentoPlanejado(UUID id, String descricao, TipoLancamentoPlanejado tipo,
            Dinheiro valor, LocalDate vencimento, StatusLancamentoPlanejado status,
            UUID contaOrigemId, UUID contaDestinoId, CategoriaDoLancamento categoria,
            UUID cartaoManualId, UUID transacaoId, MarcacaoPlanejamento marcacaoPlanejamento) {
        this(id, descricao, tipo, valor, vencimento, status, contaOrigemId, contaDestinoId,
                categoria, cartaoManualId, transacaoId, marcacaoPlanejamento, null);
    }

    public LancamentoPlanejado {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(valor);
        Objects.requireNonNull(vencimento);
        Objects.requireNonNull(status);
        marcacaoPlanejamento = marcacaoPlanejamento == null
                ? MarcacaoPlanejamento.NENHUMA : marcacaoPlanejamento;
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descrição não pode ser vazia");
        }
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        if (tipo == TipoLancamentoPlanejado.DESPESA
                && marcacaoPlanejamento == MarcacaoPlanejamento.RECEITA_RECORRENTE) {
            throw new IllegalArgumentException("receita recorrente só pode marcar receitas");
        }
        if (tipo == TipoLancamentoPlanejado.RECEITA
                && marcacaoPlanejamento != MarcacaoPlanejamento.NENHUMA
                && marcacaoPlanejamento != MarcacaoPlanejamento.RECEITA_RECORRENTE) {
            throw new IllegalArgumentException("custo fixo e piso humano só podem marcar despesas");
        }
        if (tipo == TipoLancamentoPlanejado.TRANSFERENCIA
                && marcacaoPlanejamento != MarcacaoPlanejamento.NENHUMA) {
            throw new IllegalArgumentException("transferência não possui marcação de recorrência");
        }
        if (tipo != TipoLancamentoPlanejado.RECEITA && origemReceita != null) {
            throw new IllegalArgumentException("origem da receita só pode ser usada em receitas");
        }
    }

    public StatusVisualVencimento statusEm(LocalDate hoje) {
        if (status != StatusLancamentoPlanejado.PENDENTE) return StatusVisualVencimento.ENCERRADA;
        if (vencimento.isBefore(hoje)) return StatusVisualVencimento.ATRASADA;
        if (vencimento.equals(hoje)) return StatusVisualVencimento.VENCE_HOJE;
        return StatusVisualVencimento.PROXIMA;
    }

    public LancamentoPlanejado liquidar() {
        if (status != StatusLancamentoPlanejado.PENDENTE) {
            throw new IllegalStateException("lançamento já encerrado");
        }
        return comStatus(StatusLancamentoPlanejado.LIQUIDADO);
    }

    public LancamentoPlanejado reabrir() {
        if (status != StatusLancamentoPlanejado.LIQUIDADO) {
            throw new IllegalStateException("só lançamento liquidado pode ser reaberto");
        }
        return comStatus(StatusLancamentoPlanejado.PENDENTE);
    }

    public LancamentoPlanejado cancelar() {
        if (status == StatusLancamentoPlanejado.CANCELADO) return this;
        if (status == StatusLancamentoPlanejado.LIQUIDADO) {
            throw new IllegalStateException("reabra o lançamento antes de cancelar");
        }
        return comStatus(StatusLancamentoPlanejado.CANCELADO);
    }

    public LancamentoPlanejado comStatus(StatusLancamentoPlanejado novo) {
        return new LancamentoPlanejado(id, descricao, tipo, valor, vencimento, novo,
                contaOrigemId, contaDestinoId, categoria, cartaoManualId, transacaoId,
                marcacaoPlanejamento, origemReceita);
    }
}

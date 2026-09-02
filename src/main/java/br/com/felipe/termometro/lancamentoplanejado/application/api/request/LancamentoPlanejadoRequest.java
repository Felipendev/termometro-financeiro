package br.com.felipe.termometro.lancamentoplanejado.application.api.request;

import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.EscopoEdicaoRecorrencia;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.OrigemReceita;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoPlanejadoRequest(
        @NotBlank String descricao,
        @NotBlank String tipo,
        @NotNull @Positive BigDecimal valor,
        @NotNull LocalDate vencimento,
        UUID contaOrigemId,
        UUID contaDestinoId,
        String categoria,
        String grupoCategoria,
        String naturezaCategoria,
        UUID cartaoManualId,
        String marcacaoPlanejamento,
        String origemReceita,
        @Min(1) @Max(31) Integer diaRecorrencia,
        String escopoEdicao) {

    public LancamentoPlanejadoRequest(String descricao, String tipo, BigDecimal valor,
            LocalDate vencimento, UUID contaOrigemId, UUID contaDestinoId, String categoria,
            String grupoCategoria, String naturezaCategoria, UUID cartaoManualId) {
        this(descricao, tipo, valor, vencimento, contaOrigemId, contaDestinoId, categoria,
                grupoCategoria, naturezaCategoria, cartaoManualId, null, null, null, null);
    }

    public LancamentoPlanejadoRequest(String descricao, String tipo, BigDecimal valor,
            LocalDate vencimento, UUID contaOrigemId, UUID contaDestinoId, String categoria,
            String grupoCategoria, String naturezaCategoria, UUID cartaoManualId,
            String marcacaoPlanejamento) {
        this(descricao, tipo, valor, vencimento, contaOrigemId, contaDestinoId, categoria,
                grupoCategoria, naturezaCategoria, cartaoManualId, marcacaoPlanejamento, null,
                null, null);
    }

    public LancamentoPlanejadoRequest(String descricao, String tipo, BigDecimal valor,
            LocalDate vencimento, UUID contaOrigemId, UUID contaDestinoId, String categoria,
            String grupoCategoria, String naturezaCategoria, UUID cartaoManualId,
            String marcacaoPlanejamento, String origemReceita) {
        this(descricao, tipo, valor, vencimento, contaOrigemId, contaDestinoId, categoria,
                grupoCategoria, naturezaCategoria, cartaoManualId, marcacaoPlanejamento,
                origemReceita, null, null);
    }

    public LancamentoPlanejado paraDominio(UUID id) {
        TipoLancamentoPlanejado tipoDominio = TipoLancamentoPlanejado.valueOf(tipo);
        if (tipoDominio == TipoLancamentoPlanejado.TRANSFERENCIA
                && (contaOrigemId == null || contaDestinoId == null || contaOrigemId.equals(contaDestinoId))) {
            throw new IllegalArgumentException("transferência exige contas de origem e destino diferentes");
        }
        if (tipoDominio == TipoLancamentoPlanejado.TRANSFERENCIA && diaRecorrencia != null) {
            throw new IllegalArgumentException("transferência não pode ser recorrente");
        }
        return new LancamentoPlanejado(id, descricao, tipoDominio, Dinheiro.de(valor), vencimento,
                StatusLancamentoPlanejado.PENDENTE, contaOrigemId, contaDestinoId,
                categoriaDoLancamento(tipoDominio), cartaoManualId, null, marcacao(),
                origemDaReceita(tipoDominio), null, diaRecorrencia);
    }

    public EscopoEdicaoRecorrencia escopo() {
        return escopoEdicao == null || escopoEdicao.isBlank()
                ? EscopoEdicaoRecorrencia.ESTA : EscopoEdicaoRecorrencia.valueOf(escopoEdicao);
    }

    private MarcacaoPlanejamento marcacao() {
        return marcacaoPlanejamento == null || marcacaoPlanejamento.isBlank()
                ? MarcacaoPlanejamento.NENHUMA
                : MarcacaoPlanejamento.valueOf(marcacaoPlanejamento);
    }

    private CategoriaDoLancamento categoriaDoLancamento(TipoLancamentoPlanejado tipoDominio) {
        if (tipoDominio == TipoLancamentoPlanejado.RECEITA) {
            if (categoria != null || grupoCategoria != null || naturezaCategoria != null) {
                throw new IllegalArgumentException("receita não possui categoria de despesa");
            }
            return null;
        }
        if (categoria == null && grupoCategoria == null && naturezaCategoria == null) {
            return null;
        }
        if (categoria == null || grupoCategoria == null || naturezaCategoria == null) {
            throw new IllegalArgumentException("categoria, grupo e natureza devem ser informados juntos");
        }
        return new CategoriaDoLancamento(categoria, grupoCategoria, naturezaCategoria);
    }

    private OrigemReceita origemDaReceita(TipoLancamentoPlanejado tipoDominio) {
        if (tipoDominio == TipoLancamentoPlanejado.RECEITA) {
            if (origemReceita == null || origemReceita.isBlank()) {
                throw new IllegalArgumentException("receita exige uma origem");
            }
            return OrigemReceita.valueOf(origemReceita);
        }
        if (origemReceita != null && !origemReceita.isBlank()) {
            throw new IllegalArgumentException("origem da receita só pode ser usada em receitas");
        }
        return null;
    }
}

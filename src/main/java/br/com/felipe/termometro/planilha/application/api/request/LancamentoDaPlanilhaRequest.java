package br.com.felipe.termometro.planilha.application.api.request;

import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.OrigemReceita;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Contrato enxuto para editar uma célula sem expor contas, cartões ou transferências. */
public record LancamentoDaPlanilhaRequest(
        @NotBlank String descricao,
        @NotBlank String tipo,
        @NotNull @Positive BigDecimal valor,
        String categoria,
        String grupoCategoria,
        String naturezaCategoria,
        String origemReceita) {

    public LancamentoPlanejado paraDominio(UUID id, LocalDate data) {
        TipoLancamentoPlanejado tipoDominio = switch (tipo) {
            case "ENTRADA" -> TipoLancamentoPlanejado.RECEITA;
            case "SAIDA" -> TipoLancamentoPlanejado.DESPESA;
            default -> throw new IllegalArgumentException("tipo deve ser ENTRADA ou SAIDA");
        };
        return new LancamentoPlanejado(
                id, descricao.trim(), tipoDominio, Dinheiro.de(valor), data,
                StatusLancamentoPlanejado.PENDENTE, null, null,
                categoria(tipoDominio), null, null, MarcacaoPlanejamento.NENHUMA,
                origemReceita(tipoDominio));
    }

    private CategoriaDoLancamento categoria(TipoLancamentoPlanejado tipoDominio) {
        if (tipoDominio == TipoLancamentoPlanejado.RECEITA) {
            if (categoria != null || grupoCategoria != null || naturezaCategoria != null) {
                throw new IllegalArgumentException("entrada não possui categoria de despesa");
            }
            return null;
        }
        if (categoria == null || categoria.isBlank()
                || grupoCategoria == null || grupoCategoria.isBlank()
                || naturezaCategoria == null || naturezaCategoria.isBlank()) {
            throw new IllegalArgumentException("saída exige uma categoria");
        }
        return new CategoriaDoLancamento(categoria, grupoCategoria, naturezaCategoria);
    }

    private OrigemReceita origemReceita(TipoLancamentoPlanejado tipoDominio) {
        if (tipoDominio == TipoLancamentoPlanejado.DESPESA) {
            if (origemReceita != null && !origemReceita.isBlank()) {
                throw new IllegalArgumentException("origem da receita só pode ser usada em entradas");
            }
            return null;
        }
        if (origemReceita == null || origemReceita.isBlank()) {
            throw new IllegalArgumentException("entrada exige uma origem: salário, investimento ou empréstimo");
        }
        return OrigemReceita.valueOf(origemReceita);
    }
}

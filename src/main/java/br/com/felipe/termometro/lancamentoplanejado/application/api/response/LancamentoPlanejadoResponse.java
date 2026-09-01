package br.com.felipe.termometro.lancamentoplanejado.application.api.response;

import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.OrigemReceita;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.UUID;

public record LancamentoPlanejadoResponse(
        UUID id,
        String descricao,
        String tipo,
        Dinheiro valor,
        LocalDate vencimento,
        String status,
        UUID contaOrigemId,
        UUID contaDestinoId,
        CategoriaDoLancamento categoria,
        UUID cartaoManualId,
        UUID transacaoId,
        String marcacaoPlanejamento,
        String contaOuCartao,
        boolean editavel,
        String origem,
        OrigemReceita origemReceita) {

    public LancamentoPlanejadoResponse(LancamentoPlanejado lancamento) {
        this(lancamento.id(), lancamento.descricao(), lancamento.tipo().name(), lancamento.valor(),
                lancamento.vencimento(), lancamento.status().name(), lancamento.contaOrigemId(),
                lancamento.contaDestinoId(), lancamento.categoria(), lancamento.cartaoManualId(),
                lancamento.transacaoId(), lancamento.marcacaoPlanejamento().name(), null, true,
                "MANUAL", lancamento.origemReceita());
    }

    public LancamentoPlanejadoResponse(LancamentoPlanejado lancamento, String contaOuCartao,
                                       boolean editavel, String origem) {
        this(lancamento.id(), lancamento.descricao(), lancamento.tipo().name(), lancamento.valor(),
                lancamento.vencimento(), lancamento.status().name(), lancamento.contaOrigemId(),
                lancamento.contaDestinoId(), lancamento.categoria(), lancamento.cartaoManualId(),
                lancamento.transacaoId(), lancamento.marcacaoPlanejamento().name(), contaOuCartao,
                editavel, origem, lancamento.origemReceita());
    }
}

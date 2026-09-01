package br.com.felipe.termometro.ingestao.application.api.response;

import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;

public record TransacaoPropostaResponse(String data, String descricao, String valor) {

    public static TransacaoPropostaResponse de(TransacaoBruta transacao) {
        return new TransacaoPropostaResponse(
                transacao.data().toString(), transacao.descricao(), transacao.valor().paraJson());
    }
}

package br.com.felipe.termometro.ingestao.application.api.response;

import br.com.felipe.termometro.ingestao.application.service.PropostaDeImportacao;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;

/**
 * RN-27.1 — a proposta que a UI mostra antes de importar de verdade. Sem persistência: se o
 * usuário desistir aqui, nada aconteceu.
 */
public record PropostaImportacaoResponse(
        String formatoDetectado,
        boolean reconhecido,
        boolean reconciliacaoFechou,
        String totalLido,
        int transacoesEncontradas,
        List<TransacaoPropostaResponse> amostra,
        List<String> avisos,
        List<String> formatosDisponiveis) {

    private static final int TAMANHO_DA_AMOSTRA = 5;

    public static PropostaImportacaoResponse de(PropostaDeImportacao proposta) {
        if (!proposta.reconhecido()) {
            return new PropostaImportacaoResponse(null, false, false, Dinheiro.ZERO.paraJson(), 0,
                    List.of(), List.of("Não reconheci automaticamente este arquivo — escolha o banco manualmente."),
                    proposta.formatosDisponiveis());
        }
        var leitura = proposta.leitura();
        return new PropostaImportacaoResponse(
                proposta.formatoDetectado(),
                true,
                leitura.confiavel(),
                leitura.totalDeDespesas().paraJson(),
                leitura.transacoes().size(),
                leitura.transacoes().stream().limit(TAMANHO_DA_AMOSTRA).map(TransacaoPropostaResponse::de).toList(),
                leitura.avisos(),
                proposta.formatosDisponiveis());
    }
}

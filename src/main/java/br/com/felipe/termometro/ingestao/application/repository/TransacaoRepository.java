package br.com.felipe.termometro.ingestao.application.repository;

import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Porta de saída da ingestão. Implementada em {@code infra} a partir do M3. */
public interface TransacaoRepository {

    /**
     * Persiste o lote já deduplicado e devolve as transações que eram novas (as que já existiam
     * são descartadas em silêncio). A lista devolvida alimenta a RN-22 (alerta de transação alta)
     * sem precisar de uma segunda consulta ao banco. A idempotência em si é responsabilidade do
     * {@code Deduplicador} (RN-02), não do banco.
     */
    List<TransacaoBruta> salvaTodas(String identificadorDaConta, List<TransacaoBruta> transacoes);

    /**
     * Persiste movimentos manuais gerados pela liquidação de um lançamento planejado.
     * O vínculo é separado de {@code eventoId}, que pertence exclusivamente aos eventos
     * orçamentários da RN-19.
     */
    List<TransacaoBruta> salvaTodasDoLancamentoPlanejado(
            UUID lancamentoPlanejadoId,
            String identificadorDaConta,
            List<TransacaoBruta> transacoes);

    List<TransacaoBruta> buscaPorCompetencia(Competencia competencia);

    /** Desconsidera nas análises os movimentos manuais vinculados ao lançamento, sem apagá-los. */
    void ignoraMovimentosDoLancamentoPlanejado(UUID lancamentoPlanejadoId);

    /**
     * Soma o valor absoluto das transações de cartão (seção CARTAO, não ignoradas) da
     * competência, agrupada por {@code identificadorConta}. Conta sem nenhuma transação no
     * período simplesmente não aparece no mapa — quem chama decide o zero-padrão (ver
     * {@code CartaoApplicationService}).
     */
    Map<String, Dinheiro> somaGastoDeCartaoPorConta(Competencia competencia);
}

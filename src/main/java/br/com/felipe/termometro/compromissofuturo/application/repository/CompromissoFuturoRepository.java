package br.com.felipe.termometro.compromissofuturo.application.repository;

import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.compromissofuturo.domain.LancamentoParceladoAncora;
import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;

/**
 * Porta de saída do módulo. {@code diagnostico} (RN-08) e {@code projecao} (RN-09) dependem
 * apenas de {@link #buscaPorPeriodo}, nunca da geração — a geração é um passo explícito
 * (RN-04, {@code POST /v1/compromissos-futuros/gerar}), não implícito na leitura.
 */
public interface CompromissoFuturoRepository {

    /**
     * Toda transação já sincronizada que tem parcela — o insumo bruto do gerador. Sem recorte de
     * data: o volume de uma pessoa física (milhares de transações por ano, não milhões) não
     * justifica a complexidade de uma janela deslizante, e limitar a busca arriscaria perder a
     * âncora de uma parcela de compra antiga ainda em andamento (parcelamentos vão até 48x).
     */
    List<LancamentoParceladoAncora> buscaTodosLancamentosParcelados();

    /**
     * Substitui, para cada série processada nesta rodada, tudo que estava gravado pelo
     * recém-gerado — inclusive apagando sem reinserir a série cuja âncora já é a última parcela.
     */
    void reconcilia(ResultadoDaGeracao resultado);

    /** Soma dos compromissos futuros com competência no intervalo, ambos os limites inclusos. */
    List<CompromissoFuturo> buscaPorPeriodo(Competencia inicio, Competencia fim);
}

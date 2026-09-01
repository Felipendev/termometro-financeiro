package br.com.felipe.termometro.orcamento.application.api.response;

import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O que o app mostra de manhã. Espelha {@link VerbaDoDia} traduzindo só o que muda de forma:
 * a faixa vira código + leitura, e as listas viram responses próprios.
 *
 * <p>O domínio não vaza para o cliente: {@code VerbaDoDia} pode ganhar campos internos sem que o
 * contrato REST mude junto, que é a razão de existir esta camada.
 */
public record VerbaDoDiaResponse(
        LocalDate data,
        Dinheiro verbaDeHoje,
        Dinheiro verbaBase,
        Dinheiro gastoAteHoje,
        Dinheiro restanteDoMes,
        Dinheiro reservadoParaEventos,
        int diasRestantes,
        FaixaSaude faixa,
        String leituraDaFaixa,
        BigDecimal ritmo,
        boolean baixaConfianca,
        boolean verbaAcabou,
        List<AcaoPossivelResponse> podeFazer,
        List<EventoResponse> eventosProximos,
        String mensagem) {

    public VerbaDoDiaResponse(VerbaDoDia verba) {
        this(verba.data(), verba.verbaDeHoje(), verba.verbaBase(), verba.gastoAteHoje(),
                verba.restanteDoMes(), verba.reservadoParaEventos(), verba.diasRestantes(),
                verba.faixa(), verba.faixa().leitura(), verba.ritmo(), verba.baixaConfianca(),
                verba.verbaAcabou(),
                verba.podeFazer().stream().map(AcaoPossivelResponse::new).toList(),
                verba.eventosProximos().stream().map(EventoResponse::new).toList(),
                verba.mensagem());
    }
}

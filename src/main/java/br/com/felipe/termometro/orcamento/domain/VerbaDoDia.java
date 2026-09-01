package br.com.felipe.termometro.orcamento.domain;

import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * O que o usuário vê de manhã (RN-19). Três informações e nada mais: quanto dá para gastar hoje,
 * como o mês está indo, e o que fazer com isso.
 *
 * @param baixaConfianca dias 1 e 2 do mês: o ritmo é calculado mas não alerta — dois dias não
 *                       predizem trinta (edge case 33)
 */
public record VerbaDoDia(
        LocalDate data,
        Dinheiro verbaDeHoje,
        Dinheiro verbaBase,
        Dinheiro gastoAteHoje,
        Dinheiro restanteDoMes,
        Dinheiro reservadoParaEventos,
        int diasRestantes,
        FaixaSaude faixa,
        BigDecimal ritmo,
        boolean baixaConfianca,
        List<AcaoPossivel> podeFazer,
        List<Evento> eventosProximos,
        String mensagem) {

    public VerbaDoDia {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(verbaDeHoje, "verba de hoje não pode ser nula");
        Objects.requireNonNull(faixa, "faixa não pode ser nula");
        Objects.requireNonNull(mensagem, "mensagem não pode ser nula");
        podeFazer = List.copyOf(podeFazer);
        eventosProximos = List.copyOf(eventosProximos);
    }

    public boolean verbaAcabou() {
        return verbaDeHoje.ehZero();
    }
}

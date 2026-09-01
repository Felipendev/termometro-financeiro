package br.com.felipe.termometro.orcamento.application.api.response;

import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;

public record EventoResponse(LocalDate data, String descricao, Dinheiro valor, boolean realizado) {

    public EventoResponse(Evento evento) {
        this(evento.data(), evento.descricao(), evento.valor(), evento.realizado());
    }
}

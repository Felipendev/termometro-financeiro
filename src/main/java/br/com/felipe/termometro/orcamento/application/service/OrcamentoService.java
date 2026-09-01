package br.com.felipe.termometro.orcamento.application.service;

import br.com.felipe.termometro.orcamento.application.api.request.EventoRequest;
import br.com.felipe.termometro.orcamento.application.api.request.VerbaMensalRequest;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;

/** Porta de entrada do orçamento. O controller conhece esta interface, nunca a implementação. */
public interface OrcamentoService {

    VerbaDoDia consultaVerbaDeHoje();

    VerbaDoDia consultaVerbaDoMes(Competencia competencia);

    VerbaMensal defineVerbaDoMes(Competencia competencia, VerbaMensalRequest request);

    Evento agendaEvento(Competencia competencia, EventoRequest request);
}

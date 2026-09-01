package br.com.felipe.termometro.orcamento.application.repository;

import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.GastoDoDia;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída do orçamento. Declarada em {@code application}, implementada em {@code infra} —
 * é o que mantém o domínio sem saber que existe Postgres.
 */
public interface OrcamentoRepository {

    VerbaMensal salva(VerbaMensal verba);

    Optional<VerbaMensal> buscaVerbaPorCompetencia(Competencia competencia);

    /** Gastos variáveis já agregados por dia, sem os vinculados a evento (RN-19). */
    List<GastoDoDia> buscaGastosDoDiaADia(Competencia competencia);

    List<Evento> buscaEventos(Competencia competencia);

    Evento salvaEvento(Competencia competencia, Evento evento);
}

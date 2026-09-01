package br.com.felipe.termometro.orcamento.application.service;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.orcamento.application.api.request.EventoRequest;
import br.com.felipe.termometro.orcamento.application.api.request.VerbaMensalRequest;
import br.com.felipe.termometro.orcamento.application.repository.OrcamentoRepository;
import br.com.felipe.termometro.orcamento.domain.CalculadoraDeVerbaDiaria;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrcamentoApplicationService implements OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final CalculadoraDeVerbaDiaria calculadoraDeVerbaDiaria;
    private final Clock relogio;

    @Override
    public VerbaDoDia consultaVerbaDeHoje() {
        log.info("[inicia] OrcamentoApplicationService - consultaVerbaDeHoje");
        VerbaDoDia verba = consultaVerbaDoMes(Competencia.atual(relogio));
        log.info("[finaliza] OrcamentoApplicationService - consultaVerbaDeHoje");
        return verba;
    }

    @Override
    public VerbaDoDia consultaVerbaDoMes(Competencia competencia) {
        log.info("[inicia] OrcamentoApplicationService - consultaVerbaDoMes");
        VerbaMensal verbaMensal = buscaVerbaOuFalha(competencia);
        VerbaDoDia verba = calculadoraDeVerbaDiaria.calcular(
                verbaMensal,
                orcamentoRepository.buscaGastosDoDiaADia(competencia),
                orcamentoRepository.buscaEventos(competencia),
                relogio);
        log.info("[finaliza] OrcamentoApplicationService - consultaVerbaDoMes");
        return verba;
    }

    @Override
    public VerbaMensal defineVerbaDoMes(Competencia competencia, VerbaMensalRequest request) {
        log.info("[inicia] OrcamentoApplicationService - defineVerbaDoMes");
        VerbaMensal verba = converteOuFalha(competencia, request);
        VerbaMensal salva = orcamentoRepository.salva(verba);
        log.info("[finaliza] OrcamentoApplicationService - defineVerbaDoMes");
        return salva;
    }

    @Override
    public Evento agendaEvento(Competencia competencia, EventoRequest request) {
        log.info("[inicia] OrcamentoApplicationService - agendaEvento");
        if (!competencia.contem(request.data())) {
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "O evento de " + request.data() + " não pertence à competência " + competencia + ".");
        }
        Evento salvo = orcamentoRepository.salvaEvento(competencia, request.paraDominio());
        log.info("[finaliza] OrcamentoApplicationService - agendaEvento");
        return salvo;
    }

    private VerbaMensal buscaVerbaOuFalha(Competencia competencia) {
        return orcamentoRepository.buscaVerbaPorCompetencia(competencia)
                .orElseThrow(() -> APIException.build(HttpStatus.NOT_FOUND,
                        "Nenhuma verba definida para " + competencia + "."));
    }

    /**
     * O domínio protege as próprias invariantes lançando {@code IllegalArgumentException} — mas na
     * borda isso é entrada inválida do cliente, não bug nosso. Aqui a exceção é traduzida para 400
     * com a mensagem que o domínio já escreveu, em vez de virar um 500 opaco.
     */
    private VerbaMensal converteOuFalha(Competencia competencia, VerbaMensalRequest request) {
        try {
            return request.paraDominio(competencia);
        } catch (IllegalArgumentException e) {
            throw APIException.build(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}

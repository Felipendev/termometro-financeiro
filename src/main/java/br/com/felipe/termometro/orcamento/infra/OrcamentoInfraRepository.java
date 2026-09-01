package br.com.felipe.termometro.orcamento.infra;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.orcamento.application.repository.OrcamentoRepository;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.GastoDoDia;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class OrcamentoInfraRepository implements OrcamentoRepository {

    private final VerbaMensalSpringDataJpaRepository verbaMensalRepository;
    private final EventoSpringDataJpaRepository eventoRepository;
    private final GastoDiarioSpringDataJpaRepository gastoDiarioRepository;

    @Override
    @Transactional
    public VerbaMensal salva(VerbaMensal verba) {
        log.info("[inicia] OrcamentoInfraRepository - salva");
        VerbaMensalJpaEntity entidade = verbaMensalRepository.findById(verba.competencia().primeiroDia())
                .map(existente -> {
                    existente.atualizaCom(verba);
                    return existente;
                })
                .orElseGet(() -> new VerbaMensalJpaEntity(verba));
        try {
            verbaMensalRepository.save(entidade);
        } catch (DataIntegrityViolationException e) {
            // A mesma invariante da RN-20 existe no domínio e no banco. Se estourar aqui é porque
            // algum caminho de escrita não passou pelo domínio — vale falar isso alto.
            throw APIException.build(HttpStatus.BAD_REQUEST,
                    "A verba de " + verba.competencia() + " viola uma restrição do banco.", e);
        }
        log.info("[finaliza] OrcamentoInfraRepository - salva");
        return verba;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VerbaMensal> buscaVerbaPorCompetencia(Competencia competencia) {
        log.info("[inicia] OrcamentoInfraRepository - buscaVerbaPorCompetencia");
        Optional<VerbaMensal> verba = verbaMensalRepository.findById(competencia.primeiroDia())
                .map(VerbaMensalJpaEntity::paraDominio);
        log.info("[finaliza] OrcamentoInfraRepository - buscaVerbaPorCompetencia");
        return verba;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GastoDoDia> buscaGastosDoDiaADia(Competencia competencia) {
        log.info("[inicia] OrcamentoInfraRepository - buscaGastosDoDiaADia");
        List<GastoDoDia> gastos = gastoDiarioRepository
                .somaPorDia(competencia.primeiroDia(), competencia.ultimoDia())
                .stream()
                .map(linha -> new GastoDoDia(linha.getData(), Dinheiro.de(linha.getTotal())))
                .toList();
        log.info("[finaliza] OrcamentoInfraRepository - buscaGastosDoDiaADia [{} dias]", gastos.size());
        return gastos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evento> buscaEventos(Competencia competencia) {
        log.info("[inicia] OrcamentoInfraRepository - buscaEventos");
        List<Evento> eventos = eventoRepository.findByCompetenciaOrderByData(competencia.primeiroDia())
                .stream()
                .map(EventoJpaEntity::paraDominio)
                .toList();
        log.info("[finaliza] OrcamentoInfraRepository - buscaEventos");
        return eventos;
    }

    @Override
    @Transactional
    public Evento salvaEvento(Competencia competencia, Evento evento) {
        log.info("[inicia] OrcamentoInfraRepository - salvaEvento");
        EventoJpaEntity salvo = eventoRepository.save(new EventoJpaEntity(competencia, evento));
        log.info("[finaliza] OrcamentoInfraRepository - salvaEvento");
        return salvo.paraDominio();
    }
}

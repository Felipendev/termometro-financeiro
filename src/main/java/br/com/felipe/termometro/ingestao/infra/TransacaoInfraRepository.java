package br.com.felipe.termometro.ingestao.infra;

import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.ChaveDeDeduplicacao;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TransacaoInfraRepository implements TransacaoRepository {

    private final TransacaoSpringDataJpaRepository transacaoRepository;

    /**
     * A deduplicação já aconteceu no domínio (RN-02); aqui o filtro é contra o que <i>já está no
     * banco</i>, para que reimportar o mesmo arquivo não estoure a constraint de unicidade.
     *
     * <p>Consulta os hashes existentes num único SELECT em vez de tentar inserir e tratar a
     * violação: uma reimportação de 200 linhas geraria 200 exceções e 200 rollbacks.
     */
    @Override
    @Transactional
    public List<TransacaoBruta> salvaTodas(String identificadorDaConta, List<TransacaoBruta> transacoes) {
        return salvaTodas(identificadorDaConta, transacoes, null);
    }

    @Override
    @Transactional
    public List<TransacaoBruta> salvaTodasDoLancamentoPlanejado(
            UUID lancamentoPlanejadoId,
            String identificadorDaConta,
            List<TransacaoBruta> transacoes) {
        return salvaTodas(identificadorDaConta, transacoes, lancamentoPlanejadoId);
    }

    private List<TransacaoBruta> salvaTodas(
            String identificadorDaConta,
            List<TransacaoBruta> transacoes,
            UUID lancamentoPlanejadoId) {
        log.info("[inicia] TransacaoInfraRepository - salvaTodas [{} candidatas]", transacoes.size());
        if (transacoes.isEmpty()) {
            return List.of();
        }
        Set<String> hashesCandidatos = transacoes.stream()
                .map(t -> ChaveDeDeduplicacao.calcular(identificadorDaConta, t))
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> jaExistem =
                transacaoRepository.buscaHashesExistentes(identificadorDaConta, hashesCandidatos);

        List<TransacaoBruta> novas = transacoes.stream()
                .filter(t -> !jaExistem.contains(
                        ChaveDeDeduplicacao.calcular(identificadorDaConta, t)))
                .toList();

        transacaoRepository.saveAll(
                novas.stream()
                        .map(t -> new TransacaoJpaEntity(
                                identificadorDaConta, t, lancamentoPlanejadoId))
                        .toList());
        log.info("[finaliza] TransacaoInfraRepository - salvaTodas [{} novas, {} já existiam]",
                novas.size(), transacoes.size() - novas.size());
        return novas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransacaoBruta> buscaPorCompetencia(Competencia competencia) {
        log.info("[inicia] TransacaoInfraRepository - buscaPorCompetencia");
        List<TransacaoBruta> transacoes = transacaoRepository
                .findByDataBetweenOrderByDataAsc(competencia.primeiroDia(), competencia.ultimoDia())
                .stream()
                .map(TransacaoJpaEntity::paraDominio)
                .toList();
        log.info("[finaliza] TransacaoInfraRepository - buscaPorCompetencia [{}]", transacoes.size());
        return transacoes;
    }

    @Override
    @Transactional
    public void ignoraMovimentosDoLancamentoPlanejado(UUID lancamentoPlanejadoId) {
        transacaoRepository.findByLancamentoPlanejadoId(lancamentoPlanejadoId)
                .forEach(TransacaoJpaEntity::marcaIgnorada);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Dinheiro> somaGastoDeCartaoPorConta(Competencia competencia) {
        log.info("[inicia] TransacaoInfraRepository - somaGastoDeCartaoPorConta");
        Map<String, Dinheiro> soma = transacaoRepository
                .somaPorContaCartao(competencia.primeiroDia(), competencia.ultimoDia())
                .stream()
                .collect(Collectors.toMap(
                        GastoPorContaProjection::getIdentificadorConta,
                        p -> Dinheiro.de(p.getTotal()).absoluto()));
        log.info("[finaliza] TransacaoInfraRepository - somaGastoDeCartaoPorConta [{} contas]", soma.size());
        return soma;
    }
}

package br.com.felipe.termometro.naogasto.infra;

import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import br.com.felipe.termometro.naogasto.application.repository.NaoGastoRepository;
import br.com.felipe.termometro.naogasto.domain.LancamentoParaConciliar;
import br.com.felipe.termometro.shared.Dinheiro;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class NaoGastoInfraRepository implements NaoGastoRepository {

    private final NaoGastoSpringDataJpaRepository transacaoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<LancamentoParaConciliar> buscaLancamentos(LocalDate desde, LocalDate ate) {
        log.info("[inicia] NaoGastoInfraRepository - buscaLancamentos [{} a {}]", desde, ate);
        List<LancamentoParaConciliar> lancamentos = transacaoRepository.buscaNaoIgnoradas(desde, ate)
                .stream()
                .map(NaoGastoInfraRepository::paraDominio)
                .toList();
        log.info("[finaliza] NaoGastoInfraRepository - buscaLancamentos [{}]", lancamentos.size());
        return lancamentos;
    }

    /**
     * Mesma técnica de {@code TriagemInfraRepository.aplicaEtiquetas}: {@code find} + mutação
     * dentro da transação, não {@code UPDATE} em massa — evita entidade já carregada na mesma
     * transação com valor velho de {@code ignorada}.
     */
    @Override
    @Transactional
    public int marcaIgnoradas(Set<UUID> ids) {
        log.info("[inicia] NaoGastoInfraRepository - marcaIgnoradas [{}]", ids.size());
        int aplicadas = 0;
        for (UUID id : ids) {
            TransacaoJpaEntity entidade = entityManager.find(TransacaoJpaEntity.class, id);
            if (entidade == null) {
                log.warn("[NaoGastoInfraRepository] transação {} sumiu entre a leitura e a gravação, "
                        + "ignorada", id);
                continue;
            }
            entidade.marcaIgnorada();
            aplicadas++;
        }
        log.info("[finaliza] NaoGastoInfraRepository - marcaIgnoradas [{}]", aplicadas);
        return aplicadas;
    }

    private static LancamentoParaConciliar paraDominio(TransacaoJpaEntity entidade) {
        return new LancamentoParaConciliar(
                entidade.getId(), entidade.getIdentificadorConta(), entidade.getData(),
                Dinheiro.de(entidade.getValor()), entidade.getDescricao(), entidade.getCidade(),
                entidade.getSecao());
    }
}

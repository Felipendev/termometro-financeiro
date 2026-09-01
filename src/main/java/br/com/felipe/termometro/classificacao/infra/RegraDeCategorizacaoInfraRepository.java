package br.com.felipe.termometro.classificacao.infra;

import br.com.felipe.termometro.classificacao.application.repository.RegraDeCategorizacaoRepository;
import br.com.felipe.termometro.classificacao.domain.Categoria;
import br.com.felipe.termometro.classificacao.domain.RegraDeCategorizacao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RegraDeCategorizacaoInfraRepository implements RegraDeCategorizacaoRepository {

    private final RegraDeCategorizacaoSpringDataJpaRepository regraRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RegraDeCategorizacao> buscaRegrasDoUsuario() {
        log.info("[inicia] RegraDeCategorizacaoInfraRepository - buscaRegrasDoUsuario");
        List<RegraDeCategorizacao> regras = regraRepository.findAllByOrderByPrioridadeAsc()
                .stream()
                .map(RegraDeCategorizacaoJpaEntity::paraDominio)
                .toList();
        log.info("[finaliza] RegraDeCategorizacaoInfraRepository - buscaRegrasDoUsuario [{}]", regras.size());
        return regras;
    }

    @Override
    @Transactional
    public RegraDeCategorizacao salva(RegraDeCategorizacao regra) {
        log.info("[inicia] RegraDeCategorizacaoInfraRepository - salva");
        // Regra repetida não é erro: o usuário pode classificar duas transações do mesmo
        // estabelecimento sem lembrar que já ensinou o sistema. Atualiza no lugar.
        RegraDeCategorizacaoJpaEntity entidade = regraRepository
                .findByTipoAndPadraoAndOrigem(regra.tipo(), regra.padrao(), regra.origem())
                .orElseGet(() -> new RegraDeCategorizacaoJpaEntity(regra));
        RegraDeCategorizacao salva = regraRepository.save(entidade).paraDominio();
        log.info("[finaliza] RegraDeCategorizacaoInfraRepository - salva");
        return salva;
    }

    @Override
    @Transactional
    public RegraDeCategorizacao aprende(String estabelecimento, Categoria categoria) {
        return salva(RegraDeCategorizacao.aprendida(estabelecimento, categoria));
    }
}

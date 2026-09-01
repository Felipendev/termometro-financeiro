package br.com.felipe.termometro.contribuicao.infra;

import br.com.felipe.termometro.contribuicao.application.repository.ContribuicaoRepository;
import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContribuicaoInfraRepository implements ContribuicaoRepository {

    private final MetaContribuicaoSpringDataJpaRepository jpaRepository;

    @Override
    public List<MetaContribuicao> buscaTodas() {
        return jpaRepository.findAll().stream().map(this::paraDominio).toList();
    }

    @Override
    public Optional<MetaContribuicao> busca(NomeDaContribuicao nome) {
        return jpaRepository.findById(nome.name()).map(this::paraDominio);
    }

    @Override
    public MetaContribuicao salva(MetaContribuicao meta) {
        jpaRepository.save(new MetaContribuicaoJpaEntity(
                meta.nome().name(),
                meta.percentualAlvo().fracao(),
                meta.percentualAtual().fracao(),
                meta.passoIncremento().fracao()));
        return meta;
    }

    private MetaContribuicao paraDominio(MetaContribuicaoJpaEntity entidade) {
        return new MetaContribuicao(
                NomeDaContribuicao.valueOf(entidade.getNome()),
                Percentual.deFracao(entidade.getPercentualAlvo()),
                Percentual.deFracao(entidade.getPercentualAtual()),
                Percentual.deFracao(entidade.getPassoIncremento()));
    }
}

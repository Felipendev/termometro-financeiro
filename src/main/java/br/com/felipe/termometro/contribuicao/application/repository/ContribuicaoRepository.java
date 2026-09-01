package br.com.felipe.termometro.contribuicao.application.repository;

import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import java.util.List;
import java.util.Optional;

public interface ContribuicaoRepository {
    List<MetaContribuicao> buscaTodas();

    Optional<MetaContribuicao> busca(NomeDaContribuicao nome);

    MetaContribuicao salva(MetaContribuicao meta);
}

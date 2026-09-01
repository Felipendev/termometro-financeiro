package br.com.felipe.termometro.contribuicao.application.service;

import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;

public interface ContribuicaoService {
    List<MetaComProximoPasso> consulta(Competencia competencia);

    /** RN-28.1 — recalcula a proposta do zero (nunca confia num percentual vindo do cliente). */
    MetaContribuicao autoriza(NomeDaContribuicao nome, Competencia competencia);
}

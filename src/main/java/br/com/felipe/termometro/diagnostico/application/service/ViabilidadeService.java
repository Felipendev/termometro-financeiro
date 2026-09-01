package br.com.felipe.termometro.diagnostico.application.service;

import br.com.felipe.termometro.diagnostico.domain.Viabilidade;
import br.com.felipe.termometro.shared.Competencia;

/** Porta de entrada do diagnóstico. O controller conhece esta interface, nunca a implementação. */
public interface ViabilidadeService {

    Viabilidade consultaViabilidade(Competencia competencia);
}

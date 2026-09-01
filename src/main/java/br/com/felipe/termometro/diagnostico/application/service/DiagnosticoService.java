package br.com.felipe.termometro.diagnostico.application.service;

import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.shared.Competencia;

/** Porta de entrada do diagnóstico mensal (RN-08). */
public interface DiagnosticoService {

    SaldoDeSobrevivencia consultaSaldoDeSobrevivencia(Competencia competencia);
}

package br.com.felipe.termometro.compromissofuturo.application.service;

import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;

public interface CompromissoFuturoService {

    /** RN-04: recalcula e reconcilia todas as séries de parcela conhecidas. */
    ResultadoDaGeracao gera();
}

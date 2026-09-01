package br.com.felipe.termometro.projecao.application.service;

import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.shared.Competencia;

/** Porta de entrada da projeção de quitação (RN-09). */
public interface ProjecaoService {

    Projecao projeta(Competencia competenciaInicio, Estrategia estrategia, int horizonteMeses);
}

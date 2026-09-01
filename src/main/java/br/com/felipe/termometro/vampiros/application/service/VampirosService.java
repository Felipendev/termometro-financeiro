package br.com.felipe.termometro.vampiros.application.service;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.vampiros.domain.Recorrencia;
import java.util.List;

/** Porta de entrada do detector de vampiros (RN-07). */
public interface VampirosService {

    /** Recorrências detectadas na janela de 6 meses terminando em {@code ate}, inclusive. */
    List<Recorrencia> listaVampiros(Competencia ate);
}

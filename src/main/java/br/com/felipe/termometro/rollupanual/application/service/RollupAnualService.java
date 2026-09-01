package br.com.felipe.termometro.rollupanual.application.service;

import br.com.felipe.termometro.rollupanual.domain.MesDoRollup;
import java.util.List;

public interface RollupAnualService {
    List<MesDoRollup> consulta(int ano);
}

package br.com.felipe.termometro.comparativo.application.service;

import br.com.felipe.termometro.comparativo.domain.PontoComparativo;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;

public interface ComparativoService {
    List<PontoComparativo> consulta(Competencia competencia);
}

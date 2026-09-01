package br.com.felipe.termometro.lancamentoplanejado.application.repository;

import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoPlanejadoRepository {
    List<LancamentoPlanejado> buscaPendentes();
    List<LancamentoPlanejado> buscaPorCompetencia(Competencia competencia);
    LancamentoPlanejado salva(LancamentoPlanejado item);
    void remove(UUID id);
    Optional<LancamentoPlanejado> buscaPorId(UUID id);
}

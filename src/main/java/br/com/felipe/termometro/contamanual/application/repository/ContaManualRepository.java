package br.com.felipe.termometro.contamanual.application.repository;

import br.com.felipe.termometro.contamanual.domain.ContaManual;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaManualRepository {
    List<ContaManual> buscaAtivas();
    Optional<ContaManual> buscaPorId(UUID id);
    ContaManual salva(ContaManual conta);
    void remove(UUID id);
}

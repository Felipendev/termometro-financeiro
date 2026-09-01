package br.com.felipe.termometro.planilha.application.repository;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface DiarioOverrideRepository {
    Map<LocalDate, Dinheiro> buscaEntre(LocalDate de, LocalDate ate);

    void salva(LocalDate data, Dinheiro valor);

    void salvaEmSerie(LocalDate de, LocalDate ate, Dinheiro valor);

    default Optional<LocalDate> primeiraData() {
        return Optional.empty();
    }
}

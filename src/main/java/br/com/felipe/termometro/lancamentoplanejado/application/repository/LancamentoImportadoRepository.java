package br.com.felipe.termometro.lancamentoplanejado.application.repository;

import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface LancamentoImportadoRepository {
    List<LancamentoImportado> buscaPorCompetencia(Competencia competencia);

    default Optional<LocalDate> primeiraData() {
        return Optional.empty();
    }

    record LancamentoImportado(
            UUID id,
            String descricao,
            Dinheiro valorComSinal,
            LocalDate data,
            String contaOuCartao,
            @Nullable String categoria,
            @Nullable String grupo,
            @Nullable String natureza,
            String origem) { }
}

package br.com.felipe.termometro.lancamentoplanejado.application.repository;

import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

public interface LancamentoPlanejadoRepository {
    List<LancamentoPlanejado> buscaPendentes();
    List<LancamentoPlanejado> buscaPorCompetencia(Competencia competencia);
    LancamentoPlanejado salva(LancamentoPlanejado item);
    void remove(UUID id);
    Optional<LancamentoPlanejado> buscaPorId(UUID id);

    /** Todas as ocorrências (qualquer status) de uma série de recorrência, ordenadas por vencimento. */
    List<LancamentoPlanejado> buscaPorSerie(UUID serieId);

    /** Id de toda série que ainda tem pelo menos uma ocorrência PENDENTE — candidatas a reposição. */
    List<UUID> buscaSeriesComPendencia();

    /** Lançamentos marcados como recorrentes (têm dia fixo) que ficaram sem série — nenhuma
     *  ocorrência futura foi gerada pra eles. Candidatos a adoção pelo job de reposição. */
    List<LancamentoPlanejado> buscaOrfaosDeRecorrencia();

    default Optional<LocalDate> primeiraData() {
        return Optional.empty();
    }
}

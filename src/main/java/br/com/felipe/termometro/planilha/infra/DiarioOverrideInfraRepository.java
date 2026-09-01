package br.com.felipe.termometro.planilha.infra;

import br.com.felipe.termometro.planilha.application.repository.DiarioOverrideRepository;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiarioOverrideInfraRepository implements DiarioOverrideRepository {

    private final DiarioOverrideSpringDataJpaRepository jpaRepository;
    private final Clock relogio;

    @Override
    public Map<LocalDate, Dinheiro> buscaEntre(LocalDate de, LocalDate ate) {
        return jpaRepository.findByDataBetween(de, ate).stream()
                .collect(Collectors.toMap(
                        DiarioOverrideJpaEntity::getData,
                        entidade -> Dinheiro.de(entidade.getValor())));
    }

    @Override
    public void salva(LocalDate data, Dinheiro valor) {
        jpaRepository.save(new DiarioOverrideJpaEntity(data, valor.valor(), OffsetDateTime.now(relogio)));
    }

    @Override
    public void salvaEmSerie(LocalDate de, LocalDate ate, Dinheiro valor) {
        de.datesUntil(ate.plusDays(1)).forEach(dia -> salva(dia, valor));
    }

    @Override
    public Optional<LocalDate> primeiraData() {
        return jpaRepository.findFirstByOrderByDataAsc().map(DiarioOverrideJpaEntity::getData);
    }
}

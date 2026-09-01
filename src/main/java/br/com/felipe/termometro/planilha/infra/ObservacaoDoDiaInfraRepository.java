package br.com.felipe.termometro.planilha.infra;

import br.com.felipe.termometro.planilha.application.repository.ObservacaoDoDiaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObservacaoDoDiaInfraRepository implements ObservacaoDoDiaRepository {

    private final ObservacaoDoDiaSpringDataJpaRepository jpaRepository;
    private final Clock relogio;

    @Override
    public Map<LocalDate, String> buscaEntre(LocalDate de, LocalDate ate) {
        return jpaRepository.findByDataBetween(de, ate).stream()
                .collect(Collectors.toMap(ObservacaoDoDiaJpaEntity::getData, ObservacaoDoDiaJpaEntity::getTexto));
    }

    @Override
    public void salva(LocalDate data, String texto) {
        jpaRepository.save(new ObservacaoDoDiaJpaEntity(data, texto, OffsetDateTime.now(relogio)));
    }
}

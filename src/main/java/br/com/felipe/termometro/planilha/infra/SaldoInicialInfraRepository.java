package br.com.felipe.termometro.planilha.infra;

import br.com.felipe.termometro.planilha.application.repository.SaldoInicialRepository;
import br.com.felipe.termometro.planilha.domain.SaldoInicialPlanilha;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaldoInicialInfraRepository implements SaldoInicialRepository {

    private static final short ID_DO_SINGLETON = 1;

    private final SaldoInicialPlanilhaSpringDataJpaRepository jpaRepository;
    private final Clock relogio;

    @Override
    public Optional<SaldoInicialPlanilha> busca() {
        return jpaRepository.findById(ID_DO_SINGLETON)
                .map(entidade -> new SaldoInicialPlanilha(
                        entidade.getDataReferencia(), Dinheiro.de(entidade.getValor())));
    }

    @Override
    public SaldoInicialPlanilha salva(SaldoInicialPlanilha saldoInicial) {
        var entidade = new SaldoInicialPlanilhaJpaEntity(ID_DO_SINGLETON,
                saldoInicial.dataReferencia(), saldoInicial.valor().valor(), OffsetDateTime.now(relogio));
        jpaRepository.save(entidade);
        return saldoInicial;
    }
}

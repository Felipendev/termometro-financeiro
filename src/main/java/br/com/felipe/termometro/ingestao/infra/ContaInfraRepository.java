package br.com.felipe.termometro.ingestao.infra;

import br.com.felipe.termometro.ingestao.application.repository.ContaRepository;
import br.com.felipe.termometro.ingestao.domain.ContaBancaria;
import br.com.felipe.termometro.ingestao.domain.TipoDeConta;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ContaInfraRepository implements ContaRepository {

    private final ContaSpringDataJpaRepository contaRepository;

    @Override
    @Transactional
    public void salva(ContaBancaria conta) {
        ContaJpaEntity entidade = contaRepository.findByIdentificador(conta.identificador())
                .map(existente -> {
                    existente.atualiza(conta);
                    return existente;
                })
                .orElseGet(() -> new ContaJpaEntity(UUID.randomUUID(), conta));
        contaRepository.save(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContaBancaria> buscaCartoes() {
        return contaRepository.findByTipoOrderByNome(TipoDeConta.CARTAO_CREDITO).stream()
                .map(ContaJpaEntity::paraDominio)
                .toList();
    }
}

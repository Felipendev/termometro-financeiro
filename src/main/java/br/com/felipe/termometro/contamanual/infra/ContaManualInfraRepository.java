package br.com.felipe.termometro.contamanual.infra;
import br.com.felipe.termometro.contamanual.application.repository.ContaManualRepository;
import br.com.felipe.termometro.contamanual.domain.ContaManual;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository @RequiredArgsConstructor public class ContaManualInfraRepository implements ContaManualRepository {
 private final ContaManualSpringDataJpaRepository repository;
 @Transactional(readOnly=true) public List<ContaManual> buscaAtivas(){ return repository.findByAtivaTrueOrderByNome().stream().map(ContaManualJpaEntity::paraDominio).toList(); }
 @Transactional(readOnly=true) public Optional<ContaManual> buscaPorId(UUID id){ return repository.findById(id).map(ContaManualJpaEntity::paraDominio); }
 @Transactional public ContaManual salva(ContaManual conta){ return repository.save(new ContaManualJpaEntity(conta)).paraDominio(); }
 @Transactional public void remove(UUID id){ repository.findById(id).ifPresent(ContaManualJpaEntity::desativa); }
}

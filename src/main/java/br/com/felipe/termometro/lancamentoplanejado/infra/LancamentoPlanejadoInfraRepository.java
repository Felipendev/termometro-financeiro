package br.com.felipe.termometro.lancamentoplanejado.infra;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;import br.com.felipe.termometro.lancamentoplanejado.domain.*;import br.com.felipe.termometro.shared.Competencia;import java.util.*;import lombok.*;import org.springframework.stereotype.Repository;import org.springframework.transaction.annotation.Transactional;
@Repository @RequiredArgsConstructor public class LancamentoPlanejadoInfraRepository implements LancamentoPlanejadoRepository{
 private final LancamentoPlanejadoSpringDataJpaRepository repository;
 @Transactional(readOnly=true) public List<LancamentoPlanejado> buscaPendentes(){return repository.findByStatusOrderByVencimento(StatusLancamentoPlanejado.PENDENTE).stream().map(LancamentoPlanejadoJpaEntity::paraDominio).toList();}
 @Transactional(readOnly=true) public List<LancamentoPlanejado> buscaPorCompetencia(Competencia competencia){return repository.findByVencimentoBetweenOrderByVencimentoDesc(competencia.primeiroDia(),competencia.ultimoDia()).stream().map(LancamentoPlanejadoJpaEntity::paraDominio).toList();}
 @Transactional public LancamentoPlanejado salva(LancamentoPlanejado item){return repository.save(new LancamentoPlanejadoJpaEntity(item)).paraDominio();}
 @Transactional public void remove(UUID id){repository.deleteById(id);}
 @Transactional(readOnly=true) public Optional<LancamentoPlanejado> buscaPorId(UUID id){return repository.findById(id).map(LancamentoPlanejadoJpaEntity::paraDominio);}
}

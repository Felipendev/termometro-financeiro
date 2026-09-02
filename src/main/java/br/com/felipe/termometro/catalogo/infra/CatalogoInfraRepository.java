package br.com.felipe.termometro.catalogo.infra;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CatalogoInfraRepository implements CatalogoRepository {

    private final RendaSpringDataJpaRepository rendaRepository;
    private final CustoFixoItemSpringDataJpaRepository custoFixoRepository;
    private final PisoHumanoSpringDataJpaRepository pisoHumanoRepository;
    private final DividaSpringDataJpaRepository dividaRepository;
    private final DividaRotativaSpringDataJpaRepository dividaRotativaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustoFixoItem> buscaCustoFixoAtivo() {
        return custoFixoRepository.findByAtivoTrueOrderByNome().stream()
                .map(CustoFixoItemJpaEntity::paraDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PisoHumano> buscaPisoHumano() {
        return pisoHumanoRepository.findAllByOrderByCategoria().stream()
                .map(PisoHumanoJpaEntity::paraDominio)
                .toList();
    }

    /**
     * Renda declarada vale "a partir de" até ser redeclarada: sem esse fallback pra declaração
     * anterior, navegar pra um mês ainda não declarado (o próximo, tipicamente) devolvia 404 e
     * derrubava a visão geral inteira, mesmo com a renda do mês anterior valendo.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Renda> buscaRenda(Competencia competencia) {
        return rendaRepository
                .findByCompetenciaLessThanEqualOrderByCompetenciaDesc(
                        competencia.primeiroDia(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(RendaJpaEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Renda> buscaHistoricoDeRenda(Competencia ate, int quantidade) {
        return rendaRepository
                .findByCompetenciaLessThanEqualOrderByCompetenciaDesc(
                        ate.primeiroDia(), PageRequest.of(0, quantidade))
                .stream()
                .map(RendaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Divida> buscaDividasAtivas(Competencia competencia) {
        return dividaRepository
                .findByCompetenciaUltimaParcelaGreaterThanEqualOrderByNome(competencia.primeiroDia())
                .stream()
                .map(DividaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DividaRotativa> buscaDividasRotativasAtivas() {
        return dividaRotativaRepository
                .findBySaldoDevedorGreaterThanOrderByNome(BigDecimal.ZERO)
                .stream()
                .map(DividaRotativaJpaEntity::paraDominio)
                .toList();
    }

    // -------------------------------------------------------------------- escrita (fatia 13)

    @Override
    @Transactional
    public void salvaRenda(Renda renda) {
        rendaRepository.save(new RendaJpaEntity(renda));
    }

    @Override
    @Transactional
    public CustoFixoItem salvaCustoFixo(CustoFixoItem item) {
        return custoFixoRepository.save(new CustoFixoItemJpaEntity(item)).paraDominio();
    }

    @Override
    @Transactional
    public PisoHumano salvaPisoHumano(PisoHumano piso) {
        UUID id = pisoHumanoRepository.findByCategoria(piso.categoria())
                .map(PisoHumanoJpaEntity::getId)
                .orElseGet(UUID::randomUUID);
        return pisoHumanoRepository.save(new PisoHumanoJpaEntity(id, piso)).paraDominio();
    }

    @Override
    @Transactional
    public void removePisoHumano(String categoria) {
        pisoHumanoRepository.deleteByCategoria(categoria);
    }

    @Override
    @Transactional
    public Divida salvaDivida(Divida divida) {
        return dividaRepository.save(new DividaJpaEntity(divida)).paraDominio();
    }

    @Override
    @Transactional
    public void removeDivida(UUID id) {
        if (dividaRepository.existsById(id)) {
            dividaRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public DividaRotativa salvaDividaRotativa(DividaRotativa dividaRotativa) {
        return dividaRotativaRepository.save(new DividaRotativaJpaEntity(dividaRotativa)).paraDominio();
    }

    @Override
    @Transactional
    public void removeDividaRotativa(UUID id) {
        if (dividaRotativaRepository.existsById(id)) {
            dividaRotativaRepository.deleteById(id);
        }
    }
}

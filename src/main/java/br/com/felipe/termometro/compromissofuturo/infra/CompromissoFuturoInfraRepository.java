package br.com.felipe.termometro.compromissofuturo.infra;

import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.compromissofuturo.domain.ChaveDeSerie;
import br.com.felipe.termometro.compromissofuturo.domain.CompromissoFuturo;
import br.com.felipe.termometro.compromissofuturo.domain.LancamentoParceladoAncora;
import br.com.felipe.termometro.compromissofuturo.domain.ResultadoDaGeracao;
import br.com.felipe.termometro.ingestao.infra.TransacaoJpaEntity;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CompromissoFuturoInfraRepository implements CompromissoFuturoRepository {

    private final LancamentoParceladoSpringDataJpaRepository lancamentoRepository;
    private final CompromissoFuturoSpringDataJpaRepository compromissoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LancamentoParceladoAncora> buscaTodosLancamentosParcelados() {
        log.info("[inicia] CompromissoFuturoInfraRepository - buscaTodosLancamentosParcelados");
        List<LancamentoParceladoAncora> lancamentos = lancamentoRepository.buscaParceladas().stream()
                .map(CompromissoFuturoInfraRepository::paraAncora)
                .toList();
        log.info("[finaliza] CompromissoFuturoInfraRepository - buscaTodosLancamentosParcelados [{}]",
                lancamentos.size());
        return lancamentos;
    }

    /**
     * Apaga-e-reinsere por série: mais simples e mais seguro que diferenciar linha a linha, e o
     * volume por rodada (uma série tem no máximo {@code Parcela.MAXIMO_DE_PARCELAS} linhas) é
     * pequeno demais para o custo do apaga-e-reinsere importar.
     */
    @Override
    @Transactional
    public void reconcilia(ResultadoDaGeracao resultado) {
        log.info("[inicia] CompromissoFuturoInfraRepository - reconcilia [series={}, gerados={}]",
                resultado.seriesProcessadas().size(), resultado.gerados().size());
        for (ChaveDeSerie chave : resultado.seriesProcessadas()) {
            compromissoRepository.apagaSerie(chave.identificadorConta(), chave.descricaoNormalizada(),
                    chave.parcelaTotal());
        }
        List<CompromissoFuturoJpaEntity> entidades =
                resultado.gerados().stream().map(CompromissoFuturoJpaEntity::new).toList();
        compromissoRepository.saveAll(entidades);
        log.info("[finaliza] CompromissoFuturoInfraRepository - reconcilia");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompromissoFuturo> buscaPorPeriodo(Competencia inicio, Competencia fim) {
        return compromissoRepository.findByCompetenciaBetween(inicio.primeiroDia(), fim.primeiroDia()).stream()
                .map(CompromissoFuturoJpaEntity::paraDominio)
                .toList();
    }

    private static LancamentoParceladoAncora paraAncora(TransacaoJpaEntity entidade) {
        return new LancamentoParceladoAncora(entidade.getIdentificadorConta(),
                entidade.getDescricaoNormalizada(), entidade.getDescricao(), entidade.getCategoria(),
                Competencia.de(entidade.getData()), Dinheiro.de(entidade.getValor()),
                entidade.getParcelaNumero(), entidade.getParcelaTotal());
    }
}

package br.com.felipe.termometro.catalogo.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
import br.com.felipe.termometro.support.BancoDeTesteIT;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifica não só que o repositório lê certo, mas que os dados semeados na migration V3 batem
 * com o que a planilha `termometro-felipe.xlsx` (aba Premissas) já validou.
 */
@DisplayName("CatalogoInfraRepository — Postgres de verdade")
class CatalogoInfraRepositoryIT extends BancoDeTesteIT {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Autowired
    private CatalogoRepository catalogoRepository;

    @Test
    @DisplayName("custo fixo ativo soma R$ 4.264,05, como no Painel da planilha")
    void custoFixoTotalConfereComAPlanilha() {
        List<CustoFixoItem> itens = catalogoRepository.buscaCustoFixoAtivo();

        Dinheiro total = Dinheiro.somaDe(itens.stream().map(CustoFixoItem::valor).toList());

        assertThat(itens).hasSize(12);
        assertThat(total).isEqualTo(Dinheiro.de("4264.05"));
    }

    @Test
    @DisplayName("piso humano soma R$ 1.310,00, como no Painel da planilha")
    void pisoVariavelTotalConfereComAPlanilha() {
        List<PisoHumano> pisos = catalogoRepository.buscaPisoHumano();

        Dinheiro total = Dinheiro.somaDe(pisos.stream().map(PisoHumano::valorPiso).toList());

        assertThat(pisos).hasSize(6);
        assertThat(total).isEqualTo(Dinheiro.de("1310.00"));
        assertThat(pisos).extracting(PisoHumano::estimado).containsOnly(false);
    }

    @Test
    @DisplayName("renda de setembro/2026 é R$ 10.000,00, sem variação declarada")
    void rendaDeSetembro() {
        assertThat(catalogoRepository.buscaRenda(SETEMBRO))
                .hasValueSatisfying(renda -> assertThat(renda.valorLiquido()).isEqualTo(Dinheiro.de(10000)));
    }

    @Test
    @DisplayName("mês sem declaração herda a última renda declarada, em vez de vir vazio")
    void rendaDeMesNaoDeclaradoHerdaAAnterior() {
        // sem isso, navegar pra um mês ainda não declarado devolvia 404 ("Nenhuma renda declarada")
        // e derrubava a visão geral inteira, mesmo com a declaração do mês anterior valendo
        assertThat(catalogoRepository.buscaRenda(SETEMBRO.proxima()))
                .hasValueSatisfying(renda -> assertThat(renda.valorLiquido()).isEqualTo(Dinheiro.de(10000)));
    }

    @Test
    @DisplayName("antes de qualquer declaração continua vazio — 404 legítimo")
    void rendaAntesDaPrimeiraDeclaracaoContinuaVazia() {
        assertThat(catalogoRepository.buscaRenda(Competencia.de(2000, 1))).isEmpty();
    }

    @Test
    @DisplayName("histórico de renda tem só 1 mês hoje — RN-16.1 não tem base pra disparar")
    void historicoDeRendaAindaEhCurto() {
        List<Renda> historico = catalogoRepository.buscaHistoricoDeRenda(SETEMBRO, 6);

        assertThat(historico).hasSize(1);
        assertThat(historico.getFirst().competencia()).isEqualTo(SETEMBRO);
    }

    @Test
    @DisplayName("empréstimo Nubank está ativo em setembro/2026 (última parcela) e some em outubro")
    void dividaAtivaSomeDepoisDaUltimaParcela() {
        List<Divida> ativasEmSetembro = catalogoRepository.buscaDividasAtivas(SETEMBRO);
        List<Divida> ativasEmOutubro = catalogoRepository.buscaDividasAtivas(Competencia.de(2026, 10));

        assertThat(ativasEmSetembro).hasSize(1);
        assertThat(ativasEmSetembro.getFirst().valorParcela()).isEqualTo(Dinheiro.de("2058.05"));
        assertThat(ativasEmOutubro).isEmpty();
    }

    @Test
    @DisplayName("saldo rotativo do cartão é R$ 7.952,24 a 6,36% a.m., taxa medida (não estimada)")
    void dividaRotativaConfereComAPlanilha() {
        List<DividaRotativa> rotativas = catalogoRepository.buscaDividasRotativasAtivas();

        assertThat(rotativas).hasSize(1);
        DividaRotativa unica = rotativas.getFirst();
        assertThat(unica.saldoDevedor()).isEqualTo(Dinheiro.de("7952.24"));
        assertThat(unica.taxaJurosMensal().emPontos()).isEqualByComparingTo("6.3600");
        assertThat(unica.taxaEstimada()).isFalse();
    }

    // ---------------------------------------------------------------------- escrita (fatia 13)
    //
    // Cada teste de escrita usa uma competência/id/categoria que não colide com o seed da V3 (ou
    // limpa depois de si) — os testes de leitura acima dependem de contagens exatas
    // (hasSize(12), hasSize(6)...) e a ordem de execução dos métodos nesta classe não é garantida.

    @Test
    @DisplayName("salvaRenda insere na primeira chamada e atualiza na segunda, sem duplicar linha")
    void salvaRendaEhUpsert() {
        Competencia dezembro = Competencia.de(2026, 12);

        catalogoRepository.salvaRenda(new Renda(dezembro, Dinheiro.de("10000.00"), "primeira declaração"));
        assertThat(catalogoRepository.buscaRenda(dezembro))
                .hasValueSatisfying(renda -> assertThat(renda.valorLiquido()).isEqualTo(Dinheiro.de("10000.00")));

        catalogoRepository.salvaRenda(new Renda(dezembro, Dinheiro.de("10500.00"), "reajuste"));
        assertThat(catalogoRepository.buscaRenda(dezembro))
                .hasValueSatisfying(renda -> assertThat(renda.valorLiquido()).isEqualTo(Dinheiro.de("10500.00")));
    }

    @Test
    @DisplayName("salvaCustoFixo cria ativo, depois o upsert desativa — sem sobrar item ativo pros outros testes")
    void salvaCustoFixoEhUpsertEDesativarFunciona() {
        UUID id = UUID.randomUUID();
        CustoFixoItem novo = new CustoFixoItem(id, "Item de teste IT", Dinheiro.de("50.00"), "CARTAO", null, true);

        CustoFixoItem criado = catalogoRepository.salvaCustoFixo(novo);
        assertThat(criado.ativo()).isTrue();
        assertThat(catalogoRepository.buscaCustoFixoAtivo()).extracting(CustoFixoItem::id).contains(id);

        catalogoRepository.salvaCustoFixo(new CustoFixoItem(id, "Item de teste IT", Dinheiro.de("50.00"),
                "CARTAO", null, false));
        assertThat(catalogoRepository.buscaCustoFixoAtivo()).extracting(CustoFixoItem::id).doesNotContain(id);
    }

    @Test
    @DisplayName("salvaPisoHumano cria com UUID novo, reaproveita o mesmo UUID ao atualizar, remove no fim")
    void salvaPisoHumanoEhUpsertPorCategoriaERemoveFunciona() {
        String categoria = "TESTE_IT_CATEGORIA";

        PisoHumano criado = catalogoRepository.salvaPisoHumano(
                new PisoHumano(categoria, Dinheiro.de("100.00"), "primeira", false));
        assertThat(catalogoRepository.buscaPisoHumano()).extracting(PisoHumano::categoria).contains(categoria);

        PisoHumano atualizado = catalogoRepository.salvaPisoHumano(
                new PisoHumano(categoria, Dinheiro.de("150.00"), "segunda", true));
        assertThat(atualizado.valorPiso()).isEqualTo(Dinheiro.de("150.00"));
        assertThat(catalogoRepository.buscaPisoHumano())
                .filteredOn(p -> p.categoria().equals(categoria))
                .hasSize(1);

        catalogoRepository.removePisoHumano(categoria);
        assertThat(catalogoRepository.buscaPisoHumano()).extracting(PisoHumano::categoria).doesNotContain(categoria);
    }

    @Test
    @DisplayName("removePisoHumano numa categoria inexistente não lança (idempotente)")
    void removePisoHumanoIdempotente() {
        catalogoRepository.removePisoHumano("CATEGORIA_QUE_NUNCA_EXISTIU");
    }

    @Test
    @DisplayName("salvaDivida cria e aparece como ativa, removeDivida some com ela")
    void salvaDividaERemoveDividaFuncionam() {
        UUID id = UUID.randomUUID();
        Competencia dezembro = Competencia.de(2026, 12);

        catalogoRepository.salvaDivida(new Divida(id, "Dívida de teste IT", Dinheiro.de("10.00"), dezembro, null));
        assertThat(catalogoRepository.buscaDividasAtivas(dezembro)).extracting(Divida::id).contains(id);

        catalogoRepository.removeDivida(id);
        assertThat(catalogoRepository.buscaDividasAtivas(dezembro)).extracting(Divida::id).doesNotContain(id);
    }

    @Test
    @DisplayName("removeDivida num id inexistente não lança (idempotente)")
    void removeDividaIdempotente() {
        catalogoRepository.removeDivida(UUID.randomUUID());
    }

    @Test
    @DisplayName("salvaDividaRotativa cria e aparece como ativa, removeDividaRotativa some com ela")
    void salvaDividaRotativaERemoveDividaRotativaFuncionam() {
        UUID id = UUID.randomUUID();

        catalogoRepository.salvaDividaRotativa(new DividaRotativa(id, "Rotativo de teste IT", Dinheiro.de("10.00"),
                Percentual.deFracao(new BigDecimal("0.05")), true, null));
        assertThat(catalogoRepository.buscaDividasRotativasAtivas()).extracting(DividaRotativa::id).contains(id);

        catalogoRepository.removeDividaRotativa(id);
        assertThat(catalogoRepository.buscaDividasRotativasAtivas()).extracting(DividaRotativa::id).doesNotContain(id);
    }

    @Test
    @DisplayName("removeDividaRotativa num id inexistente não lança (idempotente)")
    void removeDividaRotativaIdempotente() {
        catalogoRepository.removeDividaRotativa(UUID.randomUUID());
    }
}

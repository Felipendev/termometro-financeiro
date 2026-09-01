package br.com.felipe.termometro.lancamentoplanejado.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.classificacao.application.repository.ClassificacaoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.MarcacaoPlanejamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.OrigemReceita;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.support.BancoDeTesteIT;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

class LancamentoPlanejadoInfraRepositoryIT extends BancoDeTesteIT {

    @Autowired
    private LancamentoPlanejadoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LancamentoImportadoRepository importados;

    @Autowired
    private ClassificacaoRepository classificacoes;

    @Test
    void persisteCategoriaECartaoManualNoLancamentoPendente() {
        UUID id = UUID.randomUUID();
        UUID cartaoId = UUID.randomUUID();
        CategoriaDoLancamento categoria = new CategoriaDoLancamento("MERCADO", "ALIMENTACAO", "VARIAVEL");
        repository.salva(new LancamentoPlanejado(id, "Feira", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("85.40"), LocalDate.of(2026, 8, 25), StatusLancamentoPlanejado.PENDENTE,
                null, null, categoria, cartaoId, null));

        assertThat(repository.buscaPendentes()).filteredOn(item -> item.id().equals(id)).singleElement()
                .satisfies(item -> {
                    assertThat(item.categoria()).isEqualTo(categoria);
                    assertThat(item.cartaoManualId()).isEqualTo(cartaoId);
                });
        assertThat(repository.buscaPorCompetencia(
                br.com.felipe.termometro.shared.Competencia.parse("2026-08")))
                .extracting(LancamentoPlanejado::id)
                .contains(id);
    }

    @Test
    void persisteOrigemDaReceitaSemCategoriaDeDespesa() {
        UUID id = UUID.randomUUID();
        repository.salva(new LancamentoPlanejado(id, "Salário", TipoLancamentoPlanejado.RECEITA,
                Dinheiro.de("5000.00"), LocalDate.of(2034, 1, 5),
                StatusLancamentoPlanejado.PENDENTE, null, null, null, null, null,
                MarcacaoPlanejamento.NENHUMA, OrigemReceita.SALARIO));

        assertThat(repository.buscaPorId(id)).get().satisfies(item -> {
            assertThat(item.categoria()).isNull();
            assertThat(item.origemReceita()).isEqualTo(OrigemReceita.SALARIO);
        });
    }

    @Test
    void reconciliacaoLegadaRespeitaValorTipoEQuantidadeDeLancamentos() throws Exception {
        LocalDate vencimento = LocalDate.of(2031, 3, 18);
        insereLancamentoLegado("Aluguel de teste", "DESPESA", "2200.00", vencimento);

        insereMovimentoLegado("Aluguel de teste", "-22000.00", vencimento, "duplicado-1", true);
        insereMovimentoLegado("Aluguel de teste", "-22000.00", vencimento, "duplicado-2", true);
        insereMovimentoLegado("Aluguel de teste", "-50.00", vencimento, "valor-diferente", true);
        insereMovimentoLegado("Aluguel de teste", "50.00", vencimento, "tipo-diferente", true);

        String migracao = new ClassPathResource(
                "db/migration/V18__blindar_reconciliacao_de_movimentos_legados.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        jdbcTemplate.execute(migracao);

        Integer quantidadeConciliada = jdbcTemplate.queryForObject("""
                select count(*) from transacao
                where descricao = 'Aluguel de teste' and data = ? and ignorada = true
                """, Integer.class, vencimento);
        assertThat(quantidadeConciliada).isEqualTo(1);

        Integer divergentesAtivos = jdbcTemplate.queryForObject("""
                select count(*) from transacao
                where descricao = 'Aluguel de teste' and data = ?
                  and valor in (-50.00, 50.00) and ignorada = false
                """, Integer.class, vencimento);
        assertThat(divergentesAtivos).isEqualTo(2);
    }

    @Test
    void consultaDeLancamentosImportadosExcluiMovimentosManuaisEIgnorados() {
        LocalDate data = LocalDate.of(2032, 4, 9);
        insereMovimento("Farmácia importada", "-89.90", data, "csv-ativo",
                false, "nubank", "CSV");
        insereMovimento("Manual liquidado", "-40.00", data, "manual-fora",
                false, "manual-planejado", "MANUAL");
        insereMovimento("Compra ignorada", "-25.00", data, "csv-ignorado",
                true, "nubank", "CSV");

        assertThat(importados.buscaPorCompetencia(br.com.felipe.termometro.shared.Competencia.parse("2032-04")))
                .extracting(item -> item.descricao())
                .containsExactly("Farmácia importada");
    }

    @Test
    void revisaoDeCategoriaAceitaSomenteTransacaoImportada() {
        LocalDate data = LocalDate.of(2032, 5, 9);
        UUID importada = insereMovimento("Streaming importado", "-39.90", data, "csv-revisao",
                false, "nubank", "CSV");
        UUID manual = insereMovimento("Streaming manual", "-29.90", data, "manual-revisao",
                false, "manual-planejado", "MANUAL");

        assertThat(classificacoes.buscaContexto(importada)).isPresent();
        assertThat(classificacoes.buscaContexto(manual)).isEmpty();
    }

    @Test
    void possuiIndicesParaCompetenciaEContasDoLancamento() {
        var indices = jdbcTemplate.queryForList("""
                select indexname from pg_indexes
                where schemaname = 'public' and tablename = 'lancamento_planejado'
                """, String.class);

        assertThat(indices).contains(
                "idx_lancamento_planejado_vencimento_status",
                "idx_lancamento_planejado_conta_origem",
                "idx_lancamento_planejado_conta_destino");
    }

    private void insereLancamentoLegado(String descricao, String tipo, String valor, LocalDate vencimento) {
        jdbcTemplate.update("""
                insert into lancamento_planejado
                    (id, descricao, tipo, valor, vencimento, status, marcacao_planejamento)
                values (?, ?, ?, ?::numeric, ?, 'PENDENTE', 'NENHUMA')
                """, UUID.randomUUID(), descricao, tipo, valor, vencimento);
    }

    private void insereMovimentoLegado(String descricao, String valor, LocalDate data,
                                       String hashDedupe, boolean ignorada) {
        insereMovimento(descricao, valor, data, hashDedupe, ignorada,
                "manual-planejado", "MANUAL");
    }

    private UUID insereMovimento(String descricao, String valor, LocalDate data,
                                 String hashDedupe, boolean ignorada,
                                 String identificadorConta, String origem) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into transacao
                    (id, identificador_conta, data, descricao, descricao_original,
                     descricao_normalizada, valor, secao, origem, hash_dedupe, ignorada)
                values (?, ?, ?, ?, ?, ?, ?::numeric,
                        'MOVIMENTO_CONTA', ?, ?, ?)
                """, id, identificadorConta, data, descricao, descricao,
                descricao.toLowerCase(), valor, origem, hashDedupe, ignorada);
        return id;
    }
}

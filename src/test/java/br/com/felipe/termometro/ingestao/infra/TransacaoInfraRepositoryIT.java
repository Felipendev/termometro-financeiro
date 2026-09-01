package br.com.felipe.termometro.ingestao.infra;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.support.BancoDeTesteIT;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("TransacaoInfraRepository — Postgres de verdade")
class TransacaoInfraRepositoryIT extends BancoDeTesteIT {

    private static final String CONTA = "nubank-1234";

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private LancamentoPlanejadoRepository lancamentoPlanejadoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RN-02: quatro máquinas de lavar no mesmo dia sobrevivem à constraint de unicidade")
    void ordinalSeparaCobrancasIdenticas() {
        List<TransacaoBruta> lavanderia = List.of(
                lavagem(LocalDate.of(2027, 1, 6), 0),
                lavagem(LocalDate.of(2027, 1, 6), 1),
                lavagem(LocalDate.of(2027, 1, 6), 2),
                lavagem(LocalDate.of(2027, 1, 6), 3));

        List<TransacaoBruta> novas = transacaoRepository.salvaTodas(CONTA, lavanderia);

        assertThat(novas).hasSize(4);
        assertThat(transacaoRepository.buscaPorCompetencia(Competencia.de(2027, 1))).hasSize(4);
    }

    @Test
    @DisplayName("reimportar o mesmo arquivo não cria nada e não estoura")
    void reimportacaoEhIdempotente() {
        List<TransacaoBruta> lote = List.of(
                compra(LocalDate.of(2027, 2, 3), "-64.25", "Supermercado Arruda"),
                compra(LocalDate.of(2027, 2, 4), "-38.16", "Restaurante Minerim"));

        assertThat(transacaoRepository.salvaTodas(CONTA, lote)).hasSize(2);
        assertThat(transacaoRepository.salvaTodas(CONTA, lote))
                .as("segunda importação do mesmo arquivo")
                .isEmpty();
        assertThat(transacaoRepository.buscaPorCompetencia(Competencia.de(2027, 2))).hasSize(2);
    }

    @Test
    @DisplayName("a mesma transação em contas diferentes são duas transações")
    void contasDiferentesNaoColidem() {
        TransacaoBruta compra = compra(LocalDate.of(2027, 3, 10), "-19.90", "Apple.com/Bill");

        assertThat(transacaoRepository.salvaTodas("nubank-1234", List.of(compra))).hasSize(1);
        assertThat(transacaoRepository.salvaTodas("picpay-1030", List.of(compra))).hasSize(1);
        assertThat(transacaoRepository.buscaPorCompetencia(Competencia.de(2027, 3))).hasSize(2);
    }

    @Test
    @DisplayName("valor, sinal e parcela voltam do banco intactos")
    void ciclodeIdaEVolta() {
        TransacaoBruta original = new TransacaoBruta(
                LocalDate.of(2027, 4, 5), null, "Orange Shopping", "Orange Shopping - Parcela 9/10",
                Dinheiro.de("-198.80"), "JOAO PESSOA", "vestuário", SecaoFatura.CARTAO,
                new Parcela(9, 10), Origem.CSV, 0);

        transacaoRepository.salvaTodas(CONTA, List.of(original));

        assertThat(transacaoRepository.buscaPorCompetencia(Competencia.de(2027, 4)))
                .singleElement()
                .satisfies(lida -> {
                    assertThat(lida.valor()).isEqualTo(Dinheiro.de("-198.80"));
                    assertThat(lida.ehDespesa()).isTrue();
                    assertThat(lida.parcelaOpcional()).hasValue(new Parcela(9, 10));
                    assertThat(lida.cidade()).isEqualTo("JOAO PESSOA");
                    assertThat(lida.categoriaDoBanco()).hasValue("vestuário");
                    assertThat(lida.horaConfiavel()).isFalse();
                });
    }

    @Test
    @DisplayName("lote vazio não faz nada")
    void loteVazio() {
        assertThat(transacaoRepository.salvaTodas(CONTA, List.of())).isEmpty();
    }

    @Test
    @DisplayName("movimento planejado usa vínculo próprio e pode ser ignorado ao reabrir")
    void vinculaMovimentoAoLancamentoPlanejadoSemConfundirComEventoOrcamentario() {
        UUID lancamentoId = UUID.randomUUID();
        LocalDate vencimento = LocalDate.of(2033, 6, 12);
        lancamentoPlanejadoRepository.salva(new LancamentoPlanejado(
                lancamentoId, "Condomínio", TipoLancamentoPlanejado.DESPESA,
                Dinheiro.de("480.00"), vencimento, StatusLancamentoPlanejado.PENDENTE));
        TransacaoBruta movimento = compra(vencimento, "-480.00", "Condomínio");

        assertThat(transacaoRepository.salvaTodasDoLancamentoPlanejado(
                lancamentoId, "manual-planejado", List.of(movimento))).containsExactly(movimento);

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from transacao
                where lancamento_planejado_id = ? and evento_id is null and ignorada = false
                """, Integer.class, lancamentoId)).isEqualTo(1);

        transacaoRepository.ignoraMovimentosDoLancamentoPlanejado(lancamentoId);

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from transacao
                where lancamento_planejado_id = ? and ignorada = true
                """, Integer.class, lancamentoId)).isEqualTo(1);
    }

    private static TransacaoBruta lavagem(LocalDate data, int ordinal) {
        return new TransacaoBruta(data, null, "Smartblue Jp", "Smartblue Jp", Dinheiro.de("-14.98"),
                null, null, SecaoFatura.CARTAO, null, Origem.CSV, ordinal);
    }

    private static TransacaoBruta compra(LocalDate data, String valor, String descricao) {
        return new TransacaoBruta(data, null, descricao, descricao, Dinheiro.de(valor),
                null, null, SecaoFatura.CARTAO, null, Origem.CSV, 0);
    }
}

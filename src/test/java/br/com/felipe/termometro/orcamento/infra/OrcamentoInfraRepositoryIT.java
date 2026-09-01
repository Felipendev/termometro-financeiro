package br.com.felipe.termometro.orcamento.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.orcamento.application.repository.OrcamentoRepository;
import br.com.felipe.termometro.orcamento.domain.Evento;
import br.com.felipe.termometro.orcamento.domain.GastoDoDia;
import br.com.felipe.termometro.orcamento.domain.VerbaMensal;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.support.BancoDeTesteIT;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("OrcamentoInfraRepository — Postgres de verdade")
class OrcamentoInfraRepositoryIT extends BancoDeTesteIT {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Test
    @DisplayName("salva e lê a verba do mês sem perder centavo")
    void ciclosDaVerba() {
        VerbaMensal verba = new VerbaMensal(SETEMBRO, Dinheiro.de("2039.09"), Dinheiro.de(280));

        orcamentoRepository.salva(verba);

        assertThat(orcamentoRepository.buscaVerbaPorCompetencia(SETEMBRO))
                .hasValueSatisfying(lida -> {
                    assertThat(lida.verbaVariavel()).isEqualTo(Dinheiro.de("2039.09"));
                    assertThat(lida.provisao()).isEqualTo(Dinheiro.de(280));
                    assertThat(lida.diaADia()).isEqualTo(Dinheiro.de("1759.09"));
                });
    }

    @Test
    @DisplayName("salvar de novo atualiza no lugar — existe uma verba por mês")
    void salvarDuasVezesAtualiza() {
        Competencia outubro = Competencia.de(2026, 10);
        orcamentoRepository.salva(new VerbaMensal(outubro, Dinheiro.de(3000), Dinheiro.de(250)));
        orcamentoRepository.salva(new VerbaMensal(outubro, Dinheiro.de(5034), Dinheiro.de(400)));

        assertThat(orcamentoRepository.buscaVerbaPorCompetencia(outubro))
                .hasValueSatisfying(v -> assertThat(v.verbaVariavel()).isEqualTo(Dinheiro.de(5034)));
    }

    @Test
    @DisplayName("mês sem verba devolve vazio, não explode")
    void mesSemVerba() {
        assertThat(orcamentoRepository.buscaVerbaPorCompetencia(Competencia.de(2030, 1))).isEmpty();
    }

    @Test
    @DisplayName("eventos voltam em ordem de data")
    void eventos() {
        Competencia novembro = Competencia.de(2026, 11);
        orcamentoRepository.salvaEvento(novembro,
                Evento.previsto(LocalDate.of(2026, 11, 20), "Aniversário", Dinheiro.de(150)));
        orcamentoRepository.salvaEvento(novembro,
                Evento.previsto(LocalDate.of(2026, 11, 5), "Viagem", Dinheiro.de(110)));

        assertThat(orcamentoRepository.buscaEventos(novembro))
                .extracting(Evento::descricao)
                .containsExactly("Viagem", "Aniversário");
    }

    @Test
    @DisplayName("o banco recusa provisão maior que a verba, mesmo se o domínio for contornado")
    void bancoDefendeAInvariante() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new VerbaMensal(SETEMBRO, Dinheiro.de(100), Dinheiro.de(200)))
                .withMessageContaining("dentro da verba");
    }

    @Test
    @DisplayName("RN-19: os gastos do dia a dia vêm somados por dia, com sinal positivo")
    void gastosAgregadosPorDia() {
        Competencia dezembro = Competencia.de(2026, 12);
        transacaoRepository.salvaTodas("nubank-teste", List.of(
                despesa(LocalDate.of(2026, 12, 3), "-45.00", "Supermercado Arruda", 0),
                despesa(LocalDate.of(2026, 12, 3), "-23.00", "Marmita", 0),
                despesa(LocalDate.of(2026, 12, 7), "-38.16", "Restaurante", 0),
                credito(LocalDate.of(2026, 12, 10), "2625.03", "Pagamento recebido")));

        List<GastoDoDia> gastos = orcamentoRepository.buscaGastosDoDiaADia(dezembro);

        assertThat(gastos).hasSize(2);
        assertThat(gastos.get(0)).isEqualTo(
                new GastoDoDia(LocalDate.of(2026, 12, 3), Dinheiro.de(68)));
        assertThat(gastos.get(1)).isEqualTo(
                new GastoDoDia(LocalDate.of(2026, 12, 7), Dinheiro.de("38.16")));
    }

    private static TransacaoBruta despesa(LocalDate data, String valor, String descricao, int ordinal) {
        return new TransacaoBruta(data, null, descricao, descricao, Dinheiro.de(valor),
                null, null, SecaoFatura.CARTAO, null, Origem.CSV, ordinal);
    }

    private static TransacaoBruta credito(LocalDate data, String valor, String descricao) {
        return new TransacaoBruta(data, null, descricao, descricao, Dinheiro.de(valor),
                null, null, SecaoFatura.PAGAMENTO, null, Origem.CSV, 0);
    }
}

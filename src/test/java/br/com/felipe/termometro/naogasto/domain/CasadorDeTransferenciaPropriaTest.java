package br.com.felipe.termometro.naogasto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CasadorDeTransferenciaPropriaTest {

    @Test
    @DisplayName("saída e entrada de valor oposto em contas diferentes, dentro de 1 dia, casam")
    void saidaEEntradaEmContasDiferentesCasam() {
        UUID saida = UUID.randomUUID();
        UUID entrada = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(saida, "conta-corrente", "2026-08-10", "-500.00"),
                lancamento(entrada, "conta-poupanca", "2026-08-11", "500.00"));

        Set<UUID> casados = CasadorDeTransferenciaPropria.casar(lancamentos, 1);

        assertThat(casados).containsExactlyInAnyOrder(saida, entrada);
    }

    @Test
    @DisplayName("mesmo valor oposto na MESMA conta não é transferência própria")
    void mesmaContaNaoCasa() {
        UUID saida = UUID.randomUUID();
        UUID entrada = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(saida, "conta-corrente", "2026-08-10", "-500.00"),
                lancamento(entrada, "conta-corrente", "2026-08-10", "500.00"));

        Set<UUID> casados = CasadorDeTransferenciaPropria.casar(lancamentos, 1);

        assertThat(casados).isEmpty();
    }

    @Test
    @DisplayName("fora da janela de dias não casa")
    void foraDaJanelaNaoCasa() {
        UUID saida = UUID.randomUUID();
        UUID entrada = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(saida, "conta-corrente", "2026-08-10", "-500.00"),
                lancamento(entrada, "conta-poupanca", "2026-08-13", "500.00")); // 3 dias depois

        Set<UUID> casados = CasadorDeTransferenciaPropria.casar(lancamentos, 1);

        assertThat(casados).isEmpty();
    }

    @Test
    @DisplayName("duas transferências idênticas no mesmo dia casam cada uma com seu próprio par")
    void duasTransferenciasIdenticasCasamCadaUmaComSeuPar() {
        UUID saida1 = UUID.randomUUID();
        UUID saida2 = UUID.randomUUID();
        UUID entrada1 = UUID.randomUUID();
        UUID entrada2 = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(saida1, "conta-corrente", "2026-08-10", "-500.00"),
                lancamento(saida2, "conta-corrente", "2026-08-10", "-500.00"),
                lancamento(entrada1, "conta-poupanca", "2026-08-10", "500.00"),
                lancamento(entrada2, "conta-poupanca", "2026-08-10", "500.00"));

        Set<UUID> casados = CasadorDeTransferenciaPropria.casar(lancamentos, 1);

        assertThat(casados).containsExactlyInAnyOrder(saida1, saida2, entrada1, entrada2);
    }

    @Test
    @DisplayName("sem par disponível, nada casa")
    void semParDisponivelNadaCasa() {
        UUID saida = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos =
                List.of(lancamento(saida, "conta-corrente", "2026-08-10", "-500.00"));

        Set<UUID> casados = CasadorDeTransferenciaPropria.casar(lancamentos, 1);

        assertThat(casados).isEmpty();
    }

    private static LancamentoParaConciliar lancamento(UUID id, String conta, String data, String valor) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                "transferencia", null, SecaoFatura.MOVIMENTO_CONTA);
    }
}

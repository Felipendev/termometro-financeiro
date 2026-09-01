package br.com.felipe.termometro.naogasto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CasadorDePagamentoDeFaturaTest {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.05");
    private static final Dinheiro TOLERANCIA_DINHEIRO = Dinheiro.de(TOLERANCIA);

    @Test
    @DisplayName("débito na corrente que bate com o total da fatura do mês anterior é casado")
    void debitoQueBateComATotalDaFaturaECasado() {
        UUID compra1 = UUID.randomUUID();
        UUID compra2 = UUID.randomUUID();
        UUID debito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                cartao(compra1, "itau-cartao", "2026-07-05", "-1200.00"),
                cartao(compra2, "itau-cartao", "2026-07-20", "-2000.00"),
                corrente(debito, "itau-corrente", "2026-08-05", "-3200.00"));

        Set<UUID> casados = CasadorDePagamentoDeFatura.casar(lancamentos, TOLERANCIA_DINHEIRO, 10);

        assertThat(casados).containsExactly(debito);
        // as transações do cartão continuam sendo o gasto real — nunca entram no resultado
        assertThat(casados).doesNotContain(compra1, compra2);
    }

    @Test
    @DisplayName("débito fora da janela de dias após o fechamento não casa")
    void debitoForaDaJanelaNaoCasa() {
        UUID compra = UUID.randomUUID();
        UUID debito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                cartao(compra, "itau-cartao", "2026-07-05", "-1000.00"),
                corrente(debito, "itau-corrente", "2026-08-15", "-1000.00")); // dia 15, janela é até o 10

        Set<UUID> casados = CasadorDePagamentoDeFatura.casar(lancamentos, TOLERANCIA_DINHEIRO, 10);

        assertThat(casados).isEmpty();
    }

    @Test
    @DisplayName("valor fora da tolerância não casa")
    void valorForaDaToleranciaNaoCasa() {
        UUID compra = UUID.randomUUID();
        UUID debito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                cartao(compra, "itau-cartao", "2026-07-05", "-1000.00"),
                corrente(debito, "itau-corrente", "2026-08-05", "-999.80")); // R$0,20 de diferença

        Set<UUID> casados = CasadorDePagamentoDeFatura.casar(lancamentos, TOLERANCIA_DINHEIRO, 10);

        assertThat(casados).isEmpty();
    }

    @Test
    @DisplayName("seção que não compõe total (ex.: FUTURO) não entra na soma da fatura")
    void secaoQueNaoCompoeTotalNaoEntraNaSoma() {
        UUID compraDoMes = UUID.randomUUID();
        UUID parcelaFutura = UUID.randomUUID();
        UUID debito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                cartao(compraDoMes, "itau-cartao", "2026-07-05", "-1000.00"),
                new LancamentoParaConciliar(parcelaFutura, "itau-cartao", LocalDate.parse("2026-07-10"),
                        Dinheiro.de("-500.00"), "compra parcelada", null, SecaoFatura.FUTURO),
                corrente(debito, "itau-corrente", "2026-08-05", "-1000.00"));

        Set<UUID> casados = CasadorDePagamentoDeFatura.casar(lancamentos, TOLERANCIA_DINHEIRO, 10);

        assertThat(casados).containsExactly(debito);
    }

    private static LancamentoParaConciliar cartao(UUID id, String conta, String data, String valor) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                "compra no cartao", null, SecaoFatura.CARTAO);
    }

    private static LancamentoParaConciliar corrente(UUID id, String conta, String data, String valor) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                "pagamento cartao", null, SecaoFatura.MOVIMENTO_CONTA);
    }
}

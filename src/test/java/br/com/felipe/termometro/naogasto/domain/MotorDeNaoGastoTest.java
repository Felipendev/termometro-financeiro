package br.com.felipe.termometro.naogasto.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotorDeNaoGastoTest {

    private static final Dinheiro TOLERANCIA = Dinheiro.de(new BigDecimal("0.05"));

    @Test
    @DisplayName("combina os três casadores e soma o valor total ignorado")
    void combinaOsTresCasadores() {
        // pagamento de fatura: R$ 1.000,00
        UUID compraCartao = UUID.randomUUID();
        UUID debitoFatura = UUID.randomUUID();
        // transferência: R$ 500,00
        UUID saidaTransferencia = UUID.randomUUID();
        UUID entradaTransferencia = UUID.randomUUID();
        // estorno: R$ 80,00
        UUID compraEstornada = UUID.randomUUID();
        UUID estorno = UUID.randomUUID();
        // gasto real, não deve ser tocado
        UUID gastoReal = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                cartao(compraCartao, "itau-cartao", "2026-07-05", "-1000.00"),
                corrente(debitoFatura, "itau-corrente", "2026-08-05", "-1000.00"),
                corrente(saidaTransferencia, "itau-corrente", "2026-08-10", "-500.00"),
                corrente(entradaTransferencia, "nubank-conta", "2026-08-10", "500.00"),
                lancamentoDescricao(compraEstornada, "itau-cartao", "2026-08-01", "-80.00", "LOJA Z"),
                lancamentoDescricao(estorno, "itau-cartao", "2026-08-15", "80.00", "LOJA Z"),
                cartao(gastoReal, "itau-cartao", "2026-08-03", "-45.00"));

        ResultadoDaConciliacao resultado = MotorDeNaoGasto.concilia(lancamentos, TOLERANCIA, 10, 1, 90);

        assertThat(resultado.pagamentosDeFaturaCasados()).isEqualTo(1);
        assertThat(resultado.transferenciasCasadas()).isEqualTo(1);
        assertThat(resultado.estornosCasados()).isEqualTo(1);
        assertThat(resultado.idsParaIgnorar()).containsExactlyInAnyOrder(
                debitoFatura, saidaTransferencia, entradaTransferencia, compraEstornada, estorno);
        assertThat(resultado.idsParaIgnorar()).doesNotContain(compraCartao, gastoReal);
        // 1000 (fatura) + 500 + 500 (transferência) + 80 + 80 (estorno) = 2160,00
        assertThat(resultado.valorTotalIgnorado().valor()).isEqualByComparingTo("2160.00");
        assertThat(resultado.detalhes()).hasSize(3);
    }

    @Test
    @DisplayName("nada casa: resultado vazio, sem avisos")
    void nadaCasaResultadoVazio() {
        List<LancamentoParaConciliar> lancamentos =
                List.of(cartao(UUID.randomUUID(), "itau-cartao", "2026-08-03", "-45.00"));

        ResultadoDaConciliacao resultado = MotorDeNaoGasto.concilia(lancamentos, TOLERANCIA, 10, 1, 90);

        assertThat(resultado.idsParaIgnorar()).isEmpty();
        assertThat(resultado.pagamentosDeFaturaCasados()).isZero();
        assertThat(resultado.transferenciasCasadas()).isZero();
        assertThat(resultado.estornosCasados()).isZero();
        assertThat(resultado.valorTotalIgnorado().valor()).isEqualByComparingTo("0.00");
        assertThat(resultado.detalhes()).isEmpty();
    }

    private static LancamentoParaConciliar cartao(UUID id, String conta, String data, String valor) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                "compra no cartao", null, SecaoFatura.CARTAO);
    }

    private static LancamentoParaConciliar corrente(UUID id, String conta, String data, String valor) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                "movimento conta", null, SecaoFatura.MOVIMENTO_CONTA);
    }

    private static LancamentoParaConciliar lancamentoDescricao(UUID id, String conta, String data,
            String valor, String descricao) {
        return new LancamentoParaConciliar(id, conta, LocalDate.parse(data), Dinheiro.de(valor),
                descricao, null, SecaoFatura.CARTAO);
    }
}

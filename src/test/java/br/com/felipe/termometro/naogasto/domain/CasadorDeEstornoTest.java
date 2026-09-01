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

class CasadorDeEstornoTest {

    @Test
    @DisplayName("compra e estorno do mesmo estabelecimento, dentro de 90 dias, anulam ambas")
    void compraEEstornoDoMesmoEstabelecimentoCasam() {
        UUID compra = UUID.randomUUID();
        UUID estorno = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(compra, "LOJA X", "2026-06-01", "-100.00"),
                lancamento(estorno, "LOJA X", "2026-06-20", "100.00"));

        Set<UUID> casados = CasadorDeEstorno.casar(lancamentos, 90);

        assertThat(casados).containsExactlyInAnyOrder(compra, estorno);
    }

    @Test
    @DisplayName("estabelecimentos diferentes não casam mesmo com valor oposto e data próxima")
    void estabelecimentosDiferentesNaoCasam() {
        UUID compra = UUID.randomUUID();
        UUID credito = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(compra, "LOJA X", "2026-06-01", "-100.00"),
                lancamento(credito, "LOJA Y", "2026-06-05", "100.00"));

        Set<UUID> casados = CasadorDeEstorno.casar(lancamentos, 90);

        assertThat(casados).isEmpty();
    }

    @Test
    @DisplayName("estorno além de 90 dias não casa")
    void estornoAlemDe90DiasNaoCasa() {
        UUID compra = UUID.randomUUID();
        UUID estorno = UUID.randomUUID();

        List<LancamentoParaConciliar> lancamentos = List.of(
                lancamento(compra, "LOJA X", "2026-01-01", "-100.00"),
                lancamento(estorno, "LOJA X", "2026-06-01", "100.00")); // > 90 dias depois

        Set<UUID> casados = CasadorDeEstorno.casar(lancamentos, 90);

        assertThat(casados).isEmpty();
    }

    private static LancamentoParaConciliar lancamento(UUID id, String descricao, String data, String valor) {
        return new LancamentoParaConciliar(id, "itau-cartao", LocalDate.parse(data), Dinheiro.de(valor),
                descricao, null, SecaoFatura.CARTAO);
    }
}

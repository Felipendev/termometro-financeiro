package br.com.felipe.termometro.planilha.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculadoraDeSaldoEmCascataTest {

    private static final LocalDate DIA_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate DIA_2 = LocalDate.of(2026, 9, 2);
    private static final LocalDate DIA_3 = LocalDate.of(2026, 9, 3);

    private static ItemDoDia entrada(String valor) {
        return new ItemDoDia("recebimento", Dinheiro.de(valor), TipoItemDoDia.ENTRADA, "MANUAL");
    }

    private static ItemDoDia saida(String valor) {
        return new ItemDoDia("compra", Dinheiro.de(valor), TipoItemDoDia.SAIDA, "PDF");
    }

    @Test
    void cascateiaOSaldoDiaAposDia() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1, DIA_2, DIA_3),
                Map.of(DIA_1, List.of(saida("200")), DIA_2, List.of(entrada("1000"))),
                Map.of(DIA_3, Dinheiro.de("50")),
                Map.of(),
                Dinheiro.de("100"));

        assertThat(dias.get(0).saldo()).isEqualTo(Dinheiro.de("-100"));
        assertThat(dias.get(1).saldo()).isEqualTo(Dinheiro.de("900"));
        assertThat(dias.get(2).saldo()).isEqualTo(Dinheiro.de("850"));
    }

    @Test
    void entradaESaidaSaoDerivadasDaComposicao() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1),
                Map.of(DIA_1, List.of(saida("80"), saida("100"), entrada("30"))),
                Map.of(), Map.of(), Dinheiro.ZERO);

        assertThat(dias.get(0).saida()).isEqualTo(Dinheiro.de("180"));
        assertThat(dias.get(0).entrada()).isEqualTo(Dinheiro.de("30"));
        assertThat(dias.get(0).lancamentos()).hasSize(3);
    }

    @Test
    void marcaApenasOsDiasComOverrideDeDiarioComoSobrescritos() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1, DIA_2), Map.of(), Map.of(DIA_2, Dinheiro.de("70")), Map.of(), Dinheiro.ZERO);

        assertThat(dias.get(0).diarioSobrescrito()).isFalse();
        assertThat(dias.get(1).diarioSobrescrito()).isTrue();
        assertThat(dias.get(1).diario()).isEqualTo(Dinheiro.de("70"));
    }

    @Test
    void diaSemMovimentoMantemOSaldoDoDiaAnterior() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1), Map.of(), Map.of(), Map.of(), Dinheiro.de("500"));

        assertThat(dias.get(0).saldo()).isEqualTo(Dinheiro.de("500"));
    }

    @Test
    void devolveAObservacaoDoDiaQuandoExiste() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1), Map.of(), Map.of(), Map.of(DIA_1, "hoje"), Dinheiro.ZERO);

        assertThat(dias.get(0).observacao()).isEqualTo("hoje");
    }

    @Test
    void faixaDeSaldoReflecteAFaixaDoValor() {
        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1), Map.of(), Map.of(), Map.of(), Dinheiro.de("-50"));

        assertThat(dias.get(0).faixaSaldo()).isEqualTo(FaixaDeSaldo.VERMELHO);
    }

    @Test
    void classificaUsoDeCreditoDeSaidaDeCartaoUsandoOSaldoAntesDaquelaTransacao() {
        ItemDoDia pixSuspeito = new ItemDoDia("PIX Contador", Dinheiro.de("500"), TipoItemDoDia.SAIDA, "PDF");
        ItemDoDia compraFerramenta = new ItemDoDia("Curso online", Dinheiro.de("200"), TipoItemDoDia.SAIDA, "PDF");

        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1),
                Map.of(DIA_1, List.of(pixSuspeito, compraFerramenta)),
                Map.of(), Map.of(), Dinheiro.de("100"));

        List<ItemDoDia> lancamentos = dias.get(0).lancamentos();
        // saldo antes do Pix: 100 (positivo) — mas tem "PIX" na descrição -> ATENCAO, não DEFICIT
        assertThat(lancamentos.get(0).usoDeCredito()).isEqualTo(UsoDeCredito.ATENCAO);
        // saldo antes da segunda compra: 100 - 500 = -400 (negativo), sem PIX -> ATENCAO também
        assertThat(lancamentos.get(1).usoDeCredito()).isEqualTo(UsoDeCredito.ATENCAO);
    }

    @Test
    void naoClassificaUsoDeCreditoDeLancamentoManualNemDeEntrada() {
        ItemDoDia saidaManual = new ItemDoDia("Dinheiro no bolso", Dinheiro.de("50"), TipoItemDoDia.SAIDA, "MANUAL");
        ItemDoDia umaEntrada = new ItemDoDia("Recebimento", Dinheiro.de("300"), TipoItemDoDia.ENTRADA, "PDF");

        List<DiaDaPlanilha> dias = CalculadoraDeSaldoEmCascata.calcula(
                List.of(DIA_1), Map.of(DIA_1, List.of(saidaManual, umaEntrada)), Map.of(), Map.of(), Dinheiro.ZERO);

        assertThat(dias.get(0).lancamentos().get(0).usoDeCredito()).isNull();
        assertThat(dias.get(0).lancamentos().get(1).usoDeCredito()).isNull();
    }
}

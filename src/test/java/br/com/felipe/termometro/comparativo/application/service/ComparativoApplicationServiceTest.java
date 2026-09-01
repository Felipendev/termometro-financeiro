package br.com.felipe.termometro.comparativo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.comparativo.domain.GrupoDoComparativo;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoImportadoRepository.LancamentoImportado;
import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import br.com.felipe.termometro.lancamentoplanejado.domain.CategoriaDoLancamento;
import br.com.felipe.termometro.lancamentoplanejado.domain.LancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.StatusLancamentoPlanejado;
import br.com.felipe.termometro.lancamentoplanejado.domain.TipoLancamentoPlanejado;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComparativoApplicationServiceTest {
    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock CatalogoRepository catalogo;
    @Mock LancamentoPlanejadoRepository planejados;
    @Mock LancamentoImportadoRepository importados;
    @InjectMocks ComparativoApplicationService service;

    @Test
    void usaRendaEDespesasReaisIncluindoImportacaoEExplicaCadaGrupo() {
        when(planejados.buscaPorCompetencia(SETEMBRO)).thenReturn(List.of(
                lancamento("Salário", TipoLancamentoPlanejado.RECEITA, "5000", null),
                lancamento("Aluguel do mês", TipoLancamentoPlanejado.DESPESA, "1000",
                        new CategoriaDoLancamento("Aluguel", "MORADIA", "FIXO"))));
        when(importados.buscaPorCompetencia(SETEMBRO)).thenReturn(List.of(
                new LancamentoImportado(UUID.randomUUID(), "Mercado no cartão", Dinheiro.de("-500"),
                        LocalDate.of(2026, 9, 8), "nubank", "Mercado", "ALIMENTACAO",
                        "VARIAVEL", "NUBANK_CSV"),
                new LancamentoImportado(UUID.randomUUID(), "Transferência entre contas", Dinheiro.de("1000"),
                        LocalDate.of(2026, 9, 8), "itau", "Transferência", "TRANSFERENCIA",
                        "NAO_E_GASTO", "ITAU_CSV")));

        var pontos = service.consulta(SETEMBRO);

        assertThat(pontos).extracting(ponto -> ponto.grupo())
                .containsExactlyInAnyOrder(GrupoDoComparativo.MORADIA, GrupoDoComparativo.ALIMENTACAO);
        var moradia = pontos.stream().filter(p -> p.grupo() == GrupoDoComparativo.MORADIA).findFirst().orElseThrow();
        assertThat(moradia.valorAtual()).isEqualTo(Dinheiro.de("1000"));
        assertThat(moradia.rendaReferencia()).isEqualTo(Dinheiro.de("5000"));
        assertThat(moradia.fonte()).isEqualTo("LANCAMENTOS_DO_MES");
        assertThat(moradia.itens()).singleElement().satisfies(item -> {
            assertThat(item.descricao()).isEqualTo("Aluguel do mês");
            assertThat(item.origem()).isEqualTo("MANUAL");
        });
        verify(catalogo, never()).buscaCustoFixoAtivo();
        verify(catalogo, never()).buscaPisoHumano();
    }

    private static LancamentoPlanejado lancamento(String descricao, TipoLancamentoPlanejado tipo,
            String valor, CategoriaDoLancamento categoria) {
        return new LancamentoPlanejado(UUID.randomUUID(), descricao, tipo, Dinheiro.de(valor),
                LocalDate.of(2026, 9, 5), StatusLancamentoPlanejado.LIQUIDADO,
                null, null, categoria, null, null);
    }
}

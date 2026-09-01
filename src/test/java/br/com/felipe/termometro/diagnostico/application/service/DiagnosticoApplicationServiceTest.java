package br.com.felipe.termometro.diagnostico.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.diagnostico.domain.SaldoDeSobrevivencia;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.Parcela;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticoApplicationService")
class DiagnosticoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private CompromissoFuturoRepository compromissoFuturoRepository;

    @Mock
    private TotaisMarcadosDoMes totaisMarcados;

    @BeforeEach
    void mantemCatalogoComoFallback() {
        lenient().when(totaisMarcados.marcadoOuLegado(any(), any(), any())).thenAnswer(chamada -> chamada.getArgument(2));
    }

    @Test
    @DisplayName("soma custo fixo, piso, dívidas ativas e parcelas de cartão do mês")
    void componhaTudoESoDelegaAConta() {
        when(catalogoRepository.buscaRenda(SETEMBRO))
                .thenReturn(Optional.of(new Renda(SETEMBRO, Dinheiro.de(10000), null)));
        when(catalogoRepository.buscaCustoFixoAtivo())
                .thenReturn(List.of(item("Aluguel", "2200.00")));
        when(catalogoRepository.buscaPisoHumano())
                .thenReturn(List.of(piso("Mercado", "700.00")));
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO))
                .thenReturn(List.of(divida("Empréstimo Nubank", "2058.05")));
        when(transacaoRepository.buscaPorCompetencia(SETEMBRO)).thenReturn(List.of(
                transacaoParcelada("-150.00", 3, 10),
                transacaoParcelada("-200.00", 1, 4),
                transacaoAVista("-80.00")));
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO)).thenReturn(List.of());

        SaldoDeSobrevivencia saldo = new DiagnosticoApplicationService(catalogoRepository, transacaoRepository,
                compromissoFuturoRepository, totaisMarcados).consultaSaldoDeSobrevivencia(SETEMBRO);

        assertThat(saldo.comprometidoFixo()).isEqualTo(Dinheiro.de("2550.00")); // 2200 + 150 + 200
        assertThat(saldo.minimoVariavel()).isEqualTo(Dinheiro.de("700.00"));
        assertThat(saldo.servicoDivida()).isEqualTo(Dinheiro.de("2058.05"));
        assertThat(saldo.totalComprometido()).isEqualTo(Dinheiro.de("5308.05"));
        assertThat(saldo.saldo()).isEqualTo(Dinheiro.de("4691.95"));
        assertThat(saldo.deficit()).isFalse();
    }

    @Test
    @DisplayName("sem renda declarada para o mês, 404 explícito")
    void semRendaDeclaradaEhErro() {
        when(catalogoRepository.buscaRenda(SETEMBRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DiagnosticoApplicationService(catalogoRepository, transacaoRepository,
                compromissoFuturoRepository, totaisMarcados).consultaSaldoDeSobrevivencia(SETEMBRO))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Nenhuma renda declarada");
    }

    private CustoFixoItem item(String nome, String valor) {
        return new CustoFixoItem(UUID.randomUUID(), nome, Dinheiro.de(valor), "CARTAO", null, true);
    }

    private PisoHumano piso(String categoria, String valor) {
        return new PisoHumano(categoria, Dinheiro.de(valor), null, false);
    }

    private Divida divida(String nome, String valorParcela) {
        return new Divida(UUID.randomUUID(), nome, Dinheiro.de(valorParcela), SETEMBRO, null);
    }

    private TransacaoBruta transacaoParcelada(String valor, int numero, int total) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 10), null, "Compra parcelada",
                "Compra parcelada", Dinheiro.de(valor), null, null, SecaoFatura.CARTAO,
                new Parcela(numero, total), Origem.PDF, 0);
    }

    private TransacaoBruta transacaoAVista(String valor) {
        return new TransacaoBruta(LocalDate.of(2026, 9, 5), null, "Compra à vista",
                "Compra à vista", Dinheiro.de(valor), null, null, SecaoFatura.CARTAO, null, Origem.PDF, 0);
    }
}

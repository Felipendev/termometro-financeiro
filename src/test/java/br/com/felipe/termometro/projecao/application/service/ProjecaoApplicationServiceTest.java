package br.com.felipe.termometro.projecao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.Divida;
import br.com.felipe.termometro.catalogo.domain.DividaRotativa;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.compromissofuturo.application.repository.CompromissoFuturoRepository;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.projecao.domain.Estrategia;
import br.com.felipe.termometro.projecao.domain.MesProjetado;
import br.com.felipe.termometro.projecao.domain.Projecao;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.shared.Percentual;
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
@DisplayName("ProjecaoApplicationService")
class ProjecaoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private CompromissoFuturoRepository compromissoFuturoRepository;

    @Mock
    private TotaisMarcadosDoMes totaisMarcados;

    @BeforeEach
    void mantemCatalogoComoFallback() {
        lenient().when(totaisMarcados.marcadoOuLegado(any(), any(), any())).thenAnswer(chamada -> chamada.getArgument(2));
    }

    @Test
    @DisplayName("sem renda declarada para a competência de início, 404 explícito")
    void semRendaDeclaradaEhErro() {
        when(catalogoRepository.buscaRenda(SETEMBRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ProjecaoApplicationService(catalogoRepository, compromissoFuturoRepository, totaisMarcados)
                .projeta(SETEMBRO, Estrategia.AVALANCHE, 12))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Nenhuma renda declarada");
    }

    @Test
    @DisplayName("compõe renda, custo fixo, piso, parcela de dívida fixa ativa e saldo rotativo no primeiro mês")
    void componhaOPrimeiroMesCorretamente() {
        when(catalogoRepository.buscaRenda(SETEMBRO))
                .thenReturn(Optional.of(new Renda(SETEMBRO, Dinheiro.de("10000.00"), null)));
        when(catalogoRepository.buscaCustoFixoAtivo())
                .thenReturn(List.of(item("Aluguel", "2200.00")));
        when(catalogoRepository.buscaPisoHumano())
                .thenReturn(List.of(piso("Mercado", "700.00")));
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO))
                .thenReturn(List.of(dividaFixa("Empréstimo Nubank", "2058.05", SETEMBRO)));
        when(catalogoRepository.buscaDividasRotativasAtivas())
                .thenReturn(List.of(rotativa("Rotativo cartão", "7952.24", "6.36")));
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO.mais(23)))
                .thenReturn(List.of());

        Projecao projecao = new ProjecaoApplicationService(catalogoRepository, compromissoFuturoRepository, totaisMarcados)
                .projeta(SETEMBRO, Estrategia.AVALANCHE, 12);

        MesProjetado primeiroMes = projecao.meses().get(0);
        assertThat(primeiroMes.entrada()).isEqualTo(Dinheiro.de("10000.00"));
        // saída fixa = custo fixo (2200) + parcela do empréstimo ativo em setembro (2058.05)
        assertThat(primeiroMes.saidaFixa()).isEqualTo(Dinheiro.de("4258.05"));
        assertThat(primeiroMes.saidaVariavel()).isEqualTo(Dinheiro.de("700.00"));
        assertThat(primeiroMes.disponivel()).isEqualTo(Dinheiro.de("5041.95")); // 10000 - 4258.05 - 700
        // o saldo rotativo entrou na simulação: juros e amortização não são zero
        assertThat(primeiroMes.juros().ehPositivo()).isTrue();
        assertThat(primeiroMes.amortizacao().ehPositivo()).isTrue();
    }

    @Test
    @DisplayName("a parcela da dívida fixa some da saída fixa depois da última parcela")
    void parcelaFixaSomeDepoisDaUltimaParcela() {
        when(catalogoRepository.buscaRenda(SETEMBRO))
                .thenReturn(Optional.of(new Renda(SETEMBRO, Dinheiro.de("10000.00"), null)));
        when(catalogoRepository.buscaCustoFixoAtivo())
                .thenReturn(List.of(item("Aluguel", "2200.00")));
        when(catalogoRepository.buscaPisoHumano()).thenReturn(List.of());
        when(catalogoRepository.buscaDividasAtivas(SETEMBRO))
                .thenReturn(List.of(dividaFixa("Empréstimo Nubank", "2058.05", SETEMBRO)));
        when(catalogoRepository.buscaDividasRotativasAtivas()).thenReturn(List.of());
        when(compromissoFuturoRepository.buscaPorPeriodo(SETEMBRO, SETEMBRO.mais(23)))
                .thenReturn(List.of());

        Projecao projecao = new ProjecaoApplicationService(catalogoRepository, compromissoFuturoRepository, totaisMarcados)
                .projeta(SETEMBRO, Estrategia.AVALANCHE, 3);

        assertThat(projecao.meses().get(0).saidaFixa()).isEqualTo(Dinheiro.de("4258.05")); // setembro: parcela ativa
        assertThat(projecao.meses().get(1).saidaFixa()).isEqualTo(Dinheiro.de("2200.00")); // outubro: já quitou
    }

    private CustoFixoItem item(String nome, String valor) {
        return new CustoFixoItem(UUID.randomUUID(), nome, Dinheiro.de(valor), "CARTAO", null, true);
    }

    private PisoHumano piso(String categoria, String valor) {
        return new PisoHumano(categoria, Dinheiro.de(valor), null, false);
    }

    private Divida dividaFixa(String nome, String valorParcela, Competencia competenciaUltimaParcela) {
        return new Divida(UUID.randomUUID(), nome, Dinheiro.de(valorParcela), competenciaUltimaParcela, null);
    }

    private DividaRotativa rotativa(String nome, String saldo, String taxaPontos) {
        return new DividaRotativa(UUID.randomUUID(), nome, Dinheiro.de(saldo),
                Percentual.dePontos(taxaPontos), false, null);
    }
}

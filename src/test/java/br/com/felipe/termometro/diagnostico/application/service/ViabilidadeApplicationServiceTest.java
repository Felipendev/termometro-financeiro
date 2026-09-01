package br.com.felipe.termometro.diagnostico.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.CustoFixoItem;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.catalogo.domain.Renda;
import br.com.felipe.termometro.diagnostico.domain.Veredito;
import br.com.felipe.termometro.diagnostico.domain.Viabilidade;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.lancamentoplanejado.application.service.TotaisMarcadosDoMes;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
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
@DisplayName("ViabilidadeApplicationService")
class ViabilidadeApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private TotaisMarcadosDoMes totaisMarcados;

    @BeforeEach
    void mantemCatalogoComoFallback() {
        lenient().when(totaisMarcados.marcadoOuLegado(any(), any(), any())).thenAnswer(chamada -> chamada.getArgument(2));
    }

    @Test
    @DisplayName("soma custo fixo ativo e piso humano, e delega o veredito ao domínio")
    void componhaAsPremissasESoDelega() {
        when(catalogoRepository.buscaRenda(SETEMBRO))
                .thenReturn(Optional.of(new Renda(SETEMBRO, Dinheiro.de(10000), null)));
        when(catalogoRepository.buscaCustoFixoAtivo()).thenReturn(List.of(
                item("Aluguel", "2200.00"), item("Contador", "517.67"), item("Água", "55.00")));
        when(catalogoRepository.buscaPisoHumano()).thenReturn(List.of(
                piso("Mercado", "700.00"), piso("Comer fora", "160.00")));
        when(catalogoRepository.buscaHistoricoDeRenda(SETEMBRO, 6)).thenReturn(List.of());

        Viabilidade viabilidade =
                new ViabilidadeApplicationService(catalogoRepository, totaisMarcados).consultaViabilidade(SETEMBRO);

        assertThat(viabilidade.custoFixoTotal()).isEqualTo(Dinheiro.de("2772.67"));
        assertThat(viabilidade.pisoVariavelTotal()).isEqualTo(Dinheiro.de("860.00"));
        assertThat(viabilidade.veredito()).isEqualTo(Veredito.VIAVEL);
    }

    @Test
    @DisplayName("sem renda declarada para o mês, 404 explícito — não inventa zero")
    void semRendaDeclaradaEhErro() {
        when(catalogoRepository.buscaRenda(SETEMBRO)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> new ViabilidadeApplicationService(catalogoRepository, totaisMarcados).consultaViabilidade(SETEMBRO))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Nenhuma renda declarada");
    }

    private CustoFixoItem item(String nome, String valor) {
        return new CustoFixoItem(UUID.randomUUID(), nome, Dinheiro.de(valor), "CARTAO", null, true);
    }

    private PisoHumano piso(String categoria, String valor) {
        return new PisoHumano(categoria, Dinheiro.de(valor), null, false);
    }
}

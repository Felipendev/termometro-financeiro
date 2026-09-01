package br.com.felipe.termometro.planoajuste.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.planoajuste.domain.ItemDoPlano;
import br.com.felipe.termometro.planoajuste.domain.PlanoDeAjuste;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.repository.TriagemRepository;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanoAjusteApplicationServiceTest {

    private static final BigDecimal FATOR_35 = new BigDecimal("0.35");
    private static final Competencia REFERENCIA = Competencia.de(2026, 9);
    private static final Competencia MES_1 = Competencia.de(2026, 6);
    private static final Competencia MES_2 = Competencia.de(2026, 7);
    private static final Competencia MES_3 = Competencia.de(2026, 8);

    @Mock
    private TriagemRepository triagemRepository;

    @Mock
    private CatalogoRepository catalogoRepository;

    private PlanoAjusteApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlanoAjusteApplicationService(triagemRepository, catalogoRepository);
    }

    @Test
    @DisplayName("consulta exatamente os 3 meses fechados anteriores à referência")
    void consultaOsTresMesesFechados() {
        when(catalogoRepository.buscaPisoHumano()).thenReturn(List.of());
        when(triagemRepository.buscaClassificadasDoMes(any())).thenReturn(List.of());

        service.gera(REFERENCIA, 3, FATOR_35);

        verify(triagemRepository).buscaClassificadasDoMes(MES_1);
        verify(triagemRepository).buscaClassificadasDoMes(MES_2);
        verify(triagemRepository).buscaClassificadasDoMes(MES_3);
    }

    @Test
    @DisplayName("categoria VARIAVEL com piso vira item de rampa; FIXO e NAO_E_GASTO nunca entram")
    void compoeCategoriaVariavelComPiso() {
        when(catalogoRepository.buscaPisoHumano())
                .thenReturn(List.of(new PisoHumano("RESTAURANTE", Dinheiro.de("320.00"), "declarado", false)));

        when(triagemRepository.buscaClassificadasDoMes(MES_1)).thenReturn(List.of(
                transacao("RESTAURANTE", Natureza.VARIAVEL, "1200.00"),
                transacao("ALUGUEL", Natureza.FIXO, "2000.00"),
                transacao("APORTE", Natureza.NAO_E_GASTO, "500.00")));
        when(triagemRepository.buscaClassificadasDoMes(MES_2)).thenReturn(List.of(
                transacao("RESTAURANTE", Natureza.VARIAVEL, "1240.00"),
                transacao("ALUGUEL", Natureza.FIXO, "2000.00")));
        when(triagemRepository.buscaClassificadasDoMes(MES_3)).thenReturn(List.of(
                transacao("RESTAURANTE", Natureza.VARIAVEL, "1300.00"),
                transacao("ALUGUEL", Natureza.FIXO, "2000.00")));

        PlanoDeAjuste plano = service.gera(REFERENCIA, 3, FATOR_35);

        assertThat(plano.itens()).hasSize(1);
        ItemDoPlano item = plano.itens().get(0);
        assertThat(item.categoria()).isEqualTo("RESTAURANTE");
        assertThat(item.tipo()).isEqualTo(ItemDoPlano.TipoDeItem.RAMPA_VARIAVEL);
        assertThat(item.valorAtual().valor()).isEqualByComparingTo("1240.00");
        assertThat(item.alvoFinal().valor()).isEqualByComparingTo("320.00");
        assertThat(plano.itens()).noneMatch(i -> i.categoria().equals("ALUGUEL"));
        assertThat(plano.itens()).noneMatch(i -> i.categoria().equals("APORTE"));
    }

    @Test
    @DisplayName("categoria VARIAVEL sem piso humano gera aviso e não entra no plano")
    void categoriaSemPisoGeraAviso() {
        when(catalogoRepository.buscaPisoHumano()).thenReturn(List.of());
        when(triagemRepository.buscaClassificadasDoMes(any())).thenReturn(
                List.of(transacao("HOBBY_NOVO", Natureza.VARIAVEL, "100.00")));

        PlanoDeAjuste plano = service.gera(REFERENCIA, 3, FATOR_35);

        assertThat(plano.itens()).isEmpty();
        assertThat(plano.avisos()).anyMatch(a -> a.contains("HOBBY_NOVO"));
    }

    private static TransacaoClassificada transacao(String categoria, Natureza natureza, String valor) {
        return new TransacaoClassificada(
                UUID.randomUUID(), LocalDate.of(2026, 6, 10), Dinheiro.de(valor), categoria, natureza, null);
    }
}

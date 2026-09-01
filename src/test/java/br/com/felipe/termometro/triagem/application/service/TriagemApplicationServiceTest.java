package br.com.felipe.termometro.triagem.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.catalogo.application.repository.CatalogoRepository;
import br.com.felipe.termometro.catalogo.domain.PisoHumano;
import br.com.felipe.termometro.classificacao.domain.Natureza;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.triagem.application.api.response.ResultadoDaTriagemResponse;
import br.com.felipe.termometro.triagem.application.api.response.ResumoDeCategoriaResponse;
import br.com.felipe.termometro.triagem.application.repository.TriagemRepository;
import br.com.felipe.termometro.triagem.domain.Etiqueta;
import br.com.felipe.termometro.triagem.domain.TransacaoClassificada;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriagemApplicationService")
class TriagemApplicationServiceTest {

    @Mock
    private TriagemRepository triagemRepository;

    @Mock
    private CatalogoRepository catalogoRepository;

    private TriagemApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TriagemApplicationService(triagemRepository, catalogoRepository);
    }

    @Test
    @DisplayName("executaTriagem aplica as etiquetas decididas e resume por etiqueta")
    void executaTriagemAplicaEtiquetas() {
        Competencia competencia = Competencia.de(2026, 9);
        UUID aluguelId = UUID.randomUUID();
        UUID restauranteId = UUID.randomUUID();
        TransacaoClassificada aluguel = new TransacaoClassificada(aluguelId, LocalDate.of(2026, 9, 5),
                Dinheiro.de("1500.00"), "ALUGUEL", Natureza.FIXO, null);
        TransacaoClassificada restaurante = new TransacaoClassificada(restauranteId, LocalDate.of(2026, 9, 6),
                Dinheiro.de("80.00"), "RESTAURANTE", Natureza.VARIAVEL, null);

        when(triagemRepository.buscaClassificadasDoMes(competencia)).thenReturn(List.of(aluguel, restaurante));
        when(catalogoRepository.buscaPisoHumano())
                .thenReturn(List.of(new PisoHumano("RESTAURANTE", Dinheiro.de("160.00"), "2x/mes", false)));
        when(triagemRepository.aplicaEtiquetas(anyMap())).thenReturn(2);

        ResultadoDaTriagemResponse resultado = service.executaTriagem(competencia);

        assertThat(resultado.analisadas()).isEqualTo(2);
        assertThat(resultado.triadas()).isEqualTo(2);
        assertThat(resultado.porEtiqueta().get("AZUL")).isEqualTo(2);
        verify(triagemRepository).aplicaEtiquetas(Map.of(aluguelId, Etiqueta.AZUL, restauranteId, Etiqueta.AZUL));
    }

    @Test
    @DisplayName("resumo devolve os totais por categoria")
    void resumoDevolveTotais() {
        Competencia competencia = Competencia.de(2026, 9);
        TransacaoClassificada restaurante = new TransacaoClassificada(UUID.randomUUID(), LocalDate.of(2026, 9, 6),
                Dinheiro.de("200.00"), "RESTAURANTE", Natureza.VARIAVEL, null);

        when(triagemRepository.buscaClassificadasDoMes(competencia)).thenReturn(List.of(restaurante));
        when(catalogoRepository.buscaPisoHumano())
                .thenReturn(List.of(new PisoHumano("RESTAURANTE", Dinheiro.de("160.00"), "2x/mes", false)));

        List<ResumoDeCategoriaResponse> resumo = service.resumo(competencia);

        assertThat(resumo).hasSize(1);
        assertThat(resumo.getFirst().totalAzul()).isEqualTo(Dinheiro.de("160.00"));
        assertThat(resumo.getFirst().totalAmarelo()).isEqualTo(Dinheiro.de("40.00"));
    }

    @Test
    @DisplayName("promoveParaVermelha rejeita transação que não está AMARELA")
    void promoverRejeitaSeNaoAmarela() {
        UUID id = UUID.randomUUID();
        when(triagemRepository.buscaEtiquetaAtual(id)).thenReturn(Optional.of(Etiqueta.AZUL));

        assertThatThrownBy(() -> service.promoveParaVermelha(id)).isInstanceOf(APIException.class);
    }

    @Test
    @DisplayName("promoveParaVermelha aceita transação AMARELA e delega ao repositório")
    void promoverAceitaSeAmarela() {
        UUID id = UUID.randomUUID();
        when(triagemRepository.buscaEtiquetaAtual(id)).thenReturn(Optional.of(Etiqueta.AMARELA));

        service.promoveParaVermelha(id);

        verify(triagemRepository).promoveParaVermelha(id);
    }

    @Test
    @DisplayName("promoveParaVermelha lança 404 se a transação não existe ou não foi triada")
    void promoverLanca404SeNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(triagemRepository.buscaEtiquetaAtual(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.promoveParaVermelha(id)).isInstanceOf(APIException.class);
    }
}

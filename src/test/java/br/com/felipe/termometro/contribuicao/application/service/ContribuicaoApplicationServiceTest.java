package br.com.felipe.termometro.contribuicao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.contribuicao.application.repository.ContribuicaoRepository;
import br.com.felipe.termometro.contribuicao.domain.MetaContribuicao;
import br.com.felipe.termometro.contribuicao.domain.NomeDaContribuicao;
import br.com.felipe.termometro.diagnostico.application.service.DiagnosticoService;
import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Percentual;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ContribuicaoApplicationServiceTest {

    private static final Competencia SETEMBRO = Competencia.de(2026, 9);
    private static final Competencia OUTUBRO = Competencia.de(2026, 10);

    @Mock
    private ContribuicaoRepository repository;

    @Mock
    private DiagnosticoService diagnosticoService;

    @Test
    void devolveAsMetasComInformacaoNecessariaQuandoFaltaRendaDoMesSeguinte() {
        when(repository.buscaTodas()).thenReturn(List.of(meta()));
        when(diagnosticoService.consultaSaldoDeSobrevivencia(OUTUBRO)).thenThrow(
                APIException.build(HttpStatus.NOT_FOUND, "Nenhuma renda declarada para 2026-10."));

        List<MetaComProximoPasso> resultado = servico().consulta(SETEMBRO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).meta().nome()).isEqualTo(NomeDaContribuicao.DIZIMO);
        assertThat(resultado.get(0).valorMensalAtual()).isNull();
        assertThat(resultado.get(0).proximoPasso()).isNull();
        assertThat(resultado.get(0).informacaoNecessaria())
                .contains("2026-10", "Nenhuma renda declarada");
    }

    @Test
    void explicaPorQueNaoPodeAutorizarQuandoFaltaRendaDoMesSeguinte() {
        when(repository.busca(NomeDaContribuicao.DIZIMO)).thenReturn(Optional.of(meta()));
        when(diagnosticoService.consultaSaldoDeSobrevivencia(OUTUBRO)).thenThrow(
                APIException.build(HttpStatus.NOT_FOUND, "Nenhuma renda declarada para 2026-10."));

        assertThatThrownBy(() -> servico().autoriza(NomeDaContribuicao.DIZIMO, SETEMBRO))
                .isInstanceOfSatisfying(APIException.class, excecao -> {
                    assertThat(excecao.getStatusException()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(excecao.getMessage()).contains("Não é possível autorizar", "2026-10");
                });
    }

    private ContribuicaoApplicationService servico() {
        return new ContribuicaoApplicationService(repository, diagnosticoService);
    }

    private MetaContribuicao meta() {
        return new MetaContribuicao(NomeDaContribuicao.DIZIMO, Percentual.deFracao("0.10"),
                Percentual.ZERO, Percentual.deFracao("0.02"));
    }
}

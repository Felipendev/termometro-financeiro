package br.com.felipe.termometro.vampiros.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.ingestao.application.repository.TransacaoRepository;
import br.com.felipe.termometro.ingestao.domain.Origem;
import br.com.felipe.termometro.ingestao.domain.SecaoFatura;
import br.com.felipe.termometro.ingestao.domain.TransacaoBruta;
import br.com.felipe.termometro.shared.Competencia;
import br.com.felipe.termometro.shared.Dinheiro;
import br.com.felipe.termometro.vampiros.domain.Recorrencia;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VampirosApplicationService")
class VampirosApplicationServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Test
    @DisplayName("junta 6 meses, agrupa por estabelecimento, detecta recorrência e ignora o que não é despesa")
    void detectaAssinaturaNaJanelaDeSeisMeses() {
        Competencia ate = Competencia.de(2026, 6);

        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 1)))
                .thenReturn(List.of(despesa("2026-01-05", "NETFLIX.COM", "39.90")));
        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 2)))
                .thenReturn(List.of(despesa("2026-02-04", "NETFLIX.COM", "40.90")));
        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 3)))
                .thenReturn(List.of(despesa("2026-03-06", "NETFLIX.COM", "42.40")));
        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 4)))
                .thenReturn(List.of(despesa("2026-04-05", "NETFLIX.COM", "43.90")));
        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 5)))
                .thenReturn(List.of(despesa("2026-05-05", "NETFLIX.COM", "44.90")));
        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 6)))
                .thenReturn(List.of(recebimento("2026-06-10", "3000.00"))); // não é despesa — ignorado

        List<Recorrencia> recorrencias = new VampirosApplicationService(transacaoRepository).listaVampiros(ate);

        assertThat(recorrencias).hasSize(1);
        assertThat(recorrencias.getFirst().nomeNormalizado()).isEqualTo("NETFLIX.COM");
        assertThat(recorrencias.getFirst().custoAnual()).isEqualTo(Dinheiro.de("508.80"));
    }

    @Test
    @DisplayName("sem ocorrências suficientes em nenhum grupo, devolve lista vazia")
    void semRecorrenciaDevolveVazia() {
        Competencia ate = Competencia.de(2026, 3);
        Competencia inicio = Competencia.de(2025, 10);

        when(transacaoRepository.buscaPorCompetencia(Competencia.de(2026, 1)))
                .thenReturn(List.of(despesa("2026-01-05", "COMPRA AVULSA", "100.00")));
        for (Competencia mes : inicio.ate(ate).toList()) {
            if (mes.equals(Competencia.de(2026, 1))) {
                continue;
            }
            when(transacaoRepository.buscaPorCompetencia(mes)).thenReturn(List.of());
        }

        List<Recorrencia> recorrencias = new VampirosApplicationService(transacaoRepository).listaVampiros(ate);

        assertThat(recorrencias).isEmpty();
    }

    private TransacaoBruta despesa(String data, String descricao, String valor) {
        return new TransacaoBruta(LocalDate.parse(data), null, descricao, descricao, Dinheiro.de(valor).negado(),
                null, null, SecaoFatura.CARTAO, null, Origem.PDF, 0);
    }

    private TransacaoBruta recebimento(String data, String valor) {
        return new TransacaoBruta(LocalDate.parse(data), null, "Pagamento recebido", "Pagamento recebido",
                Dinheiro.de(valor), null, null, SecaoFatura.CARTAO, null, Origem.PDF, 0);
    }
}

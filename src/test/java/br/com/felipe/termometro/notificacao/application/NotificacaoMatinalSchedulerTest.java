package br.com.felipe.termometro.notificacao.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import br.com.felipe.termometro.orcamento.application.service.OrcamentoService;
import br.com.felipe.termometro.orcamento.domain.AcaoPossivel;
import br.com.felipe.termometro.orcamento.domain.FaixaSaude;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import br.com.felipe.termometro.shared.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoMatinalScheduler")
class NotificacaoMatinalSchedulerTest {

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private CanalDeNotificacao canal;

    @Test
    @DisplayName("canal habilitado: busca a verba de hoje e envia a mensagem já pronta")
    void enviaAMensagemDaVerbaDeHoje() {
        when(canal.habilitado()).thenReturn(true);
        when(orcamentoService.consultaVerbaDeHoje()).thenReturn(verbaDeExemplo());

        new NotificacaoMatinalScheduler(orcamentoService, canal).notificaVerbaDeHoje();

        verify(canal).envia("Você tem R$ 141,64 hoje.");
    }

    @Test
    @DisplayName("canal desabilitado: nem consulta a verba, disparo é pulado")
    void pulaQuandoCanalDesabilitado() {
        when(canal.habilitado()).thenReturn(false);

        new NotificacaoMatinalScheduler(orcamentoService, canal).notificaVerbaDeHoje();

        verify(orcamentoService, never()).consultaVerbaDeHoje();
        verify(canal, never()).envia(anyString());
    }

    @Test
    @DisplayName("sem verba definida para o mês: loga e não propaga a exceção")
    void naoPropagaQuandoSemVerbaDefinida() {
        when(canal.habilitado()).thenReturn(true);
        when(orcamentoService.consultaVerbaDeHoje())
                .thenThrow(APIException.build(HttpStatus.NOT_FOUND, "Nenhuma verba definida."));

        new NotificacaoMatinalScheduler(orcamentoService, canal).notificaVerbaDeHoje();

        verify(canal, never()).envia(anyString());
    }

    private VerbaDoDia verbaDeExemplo() {
        return new VerbaDoDia(
                LocalDate.of(2026, 9, 20),
                Dinheiro.de("141.64"),
                Dinheiro.de("90.67"),
                Dinheiro.de("1162.00"),
                Dinheiro.de("1558.00"),
                Dinheiro.de("30.00"),
                11,
                FaixaSaude.IDEAL,
                new BigDecimal("0.64"),
                false,
                List.<AcaoPossivel>of(),
                List.of(),
                "Você tem R$ 141,64 hoje.");
    }
}

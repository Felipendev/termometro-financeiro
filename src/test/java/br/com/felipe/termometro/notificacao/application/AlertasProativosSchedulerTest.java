package br.com.felipe.termometro.notificacao.application;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import br.com.felipe.termometro.notificacao.application.service.AlertaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertasProativosScheduler")
class AlertasProativosSchedulerTest {

    @Mock
    private AlertaService alertaService;

    @Test
    @DisplayName("avalia marcos e eventos próximos")
    void avaliaOsDoisGatilhos() {
        new AlertasProativosScheduler(alertaService).avaliaAlertasDiarios();

        verify(alertaService).avaliaMarcos();
        verify(alertaService).avaliaEventosProximos();
    }

    @Test
    @DisplayName("falha ao avaliar marcos não impede a avaliação de eventos próximos")
    void falhaEmMarcosNaoImpedeEventos() {
        doThrow(new RuntimeException("projeção indisponível")).when(alertaService).avaliaMarcos();

        new AlertasProativosScheduler(alertaService).avaliaAlertasDiarios();

        verify(alertaService).avaliaEventosProximos();
    }
}

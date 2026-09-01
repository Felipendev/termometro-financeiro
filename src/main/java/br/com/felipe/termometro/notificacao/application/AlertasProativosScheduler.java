package br.com.felipe.termometro.notificacao.application;

import br.com.felipe.termometro.notificacao.application.service.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN-22 — os dois gatilhos que dependem só da passagem do tempo, não de sync novo: marco atingido
 * e evento próximo. Roda uma vez por dia, 7h05 no fuso de Fortaleza (5 minutos depois da
 * notificação matinal, de propósito — não disputa com ela).
 *
 * <p>Falha num gatilho nunca impede o outro de rodar, nem derruba o agendamento do dia seguinte —
 * mesma garantia que {@link NotificacaoMatinalScheduler} já dá à notificação matinal.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlertasProativosScheduler {

    private final AlertaService alertaService;

    @Scheduled(cron = "0 5 7 * * *", zone = "America/Fortaleza")
    public void avaliaAlertasDiarios() {
        log.info("[inicia] AlertasProativosScheduler - avaliaAlertasDiarios");
        try {
            alertaService.avaliaMarcos();
        } catch (RuntimeException e) {
            log.error("[AlertasProativosScheduler] falha ao avaliar marcos", e);
        }
        try {
            alertaService.avaliaEventosProximos();
        } catch (RuntimeException e) {
            log.error("[AlertasProativosScheduler] falha ao avaliar eventos próximos", e);
        }
        log.info("[finaliza] AlertasProativosScheduler - avaliaAlertasDiarios");
    }
}

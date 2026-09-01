package br.com.felipe.termometro.notificacao.application;

import br.com.felipe.termometro.handler.APIException;
import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import br.com.felipe.termometro.orcamento.application.service.OrcamentoService;
import br.com.felipe.termometro.orcamento.domain.VerbaDoDia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RN-22 / MVP M6 — "de manhã, uma notificação: quanto dá para gastar hoje, como o mês está indo".
 *
 * <p>Dispara todo dia às 7h no fuso de Fortaleza. Não é o único gatilho de RN-22 (verba baixa,
 * evento próximo, marco atingido ficam para depois do M6) — é o mínimo que já torna o sistema
 * usável sem abrir tela nenhuma.
 *
 * <p>Falha aqui nunca deve derrubar o agendamento do dia seguinte: {@link VerbaDoDia#mensagem()}
 * já existe via {@code GET /hoje} como caminho de fallback, então qualquer erro é logado e
 * engolido, nunca propagado.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificacaoMatinalScheduler {

    private final OrcamentoService orcamentoService;
    private final CanalDeNotificacao canal;

    @Scheduled(cron = "0 0 7 * * *", zone = "America/Fortaleza")
    public void notificaVerbaDeHoje() {
        log.info("[inicia] NotificacaoMatinalScheduler - notificaVerbaDeHoje");
        if (!canal.habilitado()) {
            log.warn("[NotificacaoMatinalScheduler] canal de notificação desabilitado, disparo pulado");
            return;
        }
        try {
            VerbaDoDia verba = orcamentoService.consultaVerbaDeHoje();
            canal.envia(verba.mensagem());
        } catch (APIException e) {
            log.warn("[NotificacaoMatinalScheduler] sem verba definida para o mês corrente, "
                    + "disparo pulado: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("[NotificacaoMatinalScheduler] falha inesperada ao montar a notificação matinal", e);
        }
        log.info("[finaliza] NotificacaoMatinalScheduler - notificaVerbaDeHoje");
    }
}

package br.com.felipe.termometro.lancamentoplanejado.recorrencia;

import br.com.felipe.termometro.lancamentoplanejado.application.repository.LancamentoPlanejadoRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Repõe toda série de recorrência até {@link RecorrenciaLancamentoService#HORIZONTE_MESES} meses à
 * frente — sem isso, uma série criada há um ano teria só as ocorrências geradas naquele momento e
 * ia "acabando" conforme o tempo passasse. Idempotente (nunca sobrescreve o que já existe), então
 * rodar todo dia é seguro e barato. Uma série falhando nunca impede as outras.
 *
 * <p>Roda também no start: esperar até as 3h30 pra um recorrente marcado agora aparecer nos meses
 * seguintes seria indistinguível de bug pra quem está usando.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RecorrenciaTopUpScheduler {

    private final LancamentoPlanejadoRepository repository;
    private final RecorrenciaLancamentoService recorrenciaService;

    @EventListener(ApplicationReadyEvent.class)
    public void reponhaAoSubir() {
        reponhaSeries();
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Fortaleza")
    public void reponhaSeries() {
        try {
            recorrenciaService.adotaOrfaos();
        } catch (RuntimeException e) {
            log.error("[RecorrenciaTopUpScheduler] falha ao adotar recorrentes sem série", e);
        }
        var series = repository.buscaSeriesComPendencia();
        log.info("[inicia] RecorrenciaTopUpScheduler - reponhaSeries [{} séries]", series.size());
        for (UUID serieId : series) {
            try {
                recorrenciaService.materializaAteHorizonte(serieId);
            } catch (RuntimeException e) {
                log.error("[RecorrenciaTopUpScheduler] falha ao repor série {}", serieId, e);
            }
        }
        log.info("[finaliza] RecorrenciaTopUpScheduler - reponhaSeries");
    }
}

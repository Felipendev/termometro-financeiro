package br.com.felipe.termometro.config;

import br.com.felipe.termometro.shared.Competencia;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O relógio é injetado, nunca lido de {@code LocalDate.now()} dentro de uma regra.
 *
 * <p>Run-rate (RN-10), ritmo (RN-14) e janelas de análise (RN-13) dependem de "hoje".
 * Regra que consulta o relógio do sistema direto não tem como ser testada em dia 3, dia 28
 * e dia 31 — e são exatamente esses os dias em que ela erra.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.system(Competencia.FUSO);
    }
}

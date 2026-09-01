package br.com.felipe.termometro.config;

import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import br.com.felipe.termometro.notificacao.infra.telegram.TelegramClient;
import br.com.felipe.termometro.notificacao.infra.telegram.TelegramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class NotificacaoConfig {

    /** {@code RestClient.Builder} próprio do Telegram — cada integração HTTP tem o seu. */
    @Bean
    public CanalDeNotificacao canalDeNotificacao(TelegramProperties propriedades) {
        return new TelegramClient(RestClient.builder(), propriedades);
    }
}

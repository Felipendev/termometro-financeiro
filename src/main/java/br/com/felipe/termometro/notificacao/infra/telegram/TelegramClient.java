package br.com.felipe.termometro.notificacao.infra.telegram;

import br.com.felipe.termometro.notificacao.domain.CanalDeNotificacao;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envia mensagens via {@code sendMessage} da Bot API do Telegram. Sem retry: se der errado, a
 * notificação de hoje se perde, mas {@code GET /hoje} continua disponível — o canal é conveniência,
 * não a fonte da verdade.
 *
 * <p>Construído explicitamente pelo {@code NotificacaoConfig} com um {@link RestClient.Builder}
 * próprio — não o singleton compartilhado do Pluggy — porque {@code RestClient.Builder} é mutável
 * e cada {@code .baseUrl(...)} pisaria no do outro se dividissem a mesma instância.
 */
@Slf4j
public class TelegramClient implements CanalDeNotificacao {

    private final RestClient restClient;
    private final TelegramProperties propriedades;

    public TelegramClient(RestClient.Builder builder, TelegramProperties propriedades) {
        this.restClient = builder.baseUrl(propriedades.baseUrl()).build();
        this.propriedades = propriedades;
    }

    @Override
    public boolean habilitado() {
        return propriedades.configurado();
    }

    @Override
    public void envia(String mensagem) {
        if (!habilitado()) {
            log.warn("[TelegramClient] canal não configurado (falta TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID) "
                    + "- mensagem descartada: {}", mensagem);
            return;
        }
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", propriedades.botToken())
                    .body(Map.of("chat_id", propriedades.chatId(), "text", mensagem))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[TelegramClient] mensagem enviada");
        } catch (RestClientException e) {
            log.error("[TelegramClient] falha ao enviar mensagem ao Telegram", e);
        }
    }
}

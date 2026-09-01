package br.com.felipe.termometro.notificacao.infra.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do bot do Telegram. Diferente do Pluggy, aqui não é obrigatório: o app sobe sem
 * elas, e a notificação matinal é simplesmente pulada até você criar o bot com o @BotFather e
 * definir {@code TELEGRAM_BOT_TOKEN} e {@code TELEGRAM_CHAT_ID}.
 */
@ConfigurationProperties("telegram")
public record TelegramProperties(String baseUrl, String botToken, String chatId) {

    public TelegramProperties {
        baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://api.telegram.org";
    }

    public boolean configurado() {
        return naoEmBranco(botToken) && naoEmBranco(chatId);
    }

    private static boolean naoEmBranco(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public String toString() {
        return "TelegramProperties[baseUrl=%s, botToken=%s, chatId=%s]"
                .formatted(baseUrl, mascarado(botToken), mascarado(chatId));
    }

    private static String mascarado(String valor) {
        if (valor == null || valor.isBlank()) {
            return "(não configurado)";
        }
        return valor.length() <= 4 ? "****" : valor.substring(0, 4) + "****";
    }
}

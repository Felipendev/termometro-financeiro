package br.com.felipe.termometro.notificacao.infra.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("TelegramClient")
class TelegramClientTest {

    private static final String BASE = "https://api.telegram.org";

    @Test
    @DisplayName("envia a mensagem para o sendMessage do bot configurado")
    void enviaMensagemQuandoConfigurado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties propriedades = new TelegramProperties(BASE, "meu-token", "12345");
        TelegramClient client = new TelegramClient(builder, propriedades);

        servidor.expect(requestTo(BASE + "/bot" + "meu-token" + "/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"chat_id\":\"12345\"")))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        client.envia("Você tem R$ 141,64 hoje.");

        servidor.verify();
    }

    @Test
    @DisplayName("sem token/chat_id configurados, não chama a API e não estoura")
    void naoChamaQuandoDesabilitado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties propriedades = new TelegramProperties(BASE, null, "");
        TelegramClient client = new TelegramClient(builder, propriedades);

        assertThat(client.habilitado()).isFalse();
        client.envia("qualquer coisa");

        servidor.verify();
    }

    @Test
    @DisplayName("erro do Telegram é logado e engolido, não propaga")
    void naoPropagaErroDoTelegram() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties propriedades = new TelegramProperties(BASE, "token-x", "999");
        TelegramClient client = new TelegramClient(builder, propriedades);

        servidor.expect(requestTo(BASE + "/bottoken-x/sendMessage"))
                .andRespond(withServerError());

        client.envia("mensagem que vai falhar");

        servidor.verify();
    }
}

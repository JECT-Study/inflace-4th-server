package com.example.inflace.global.client;

import com.example.inflace.global.notification.discord.payload.DiscordWebhookPayload;
import com.example.inflace.global.properties.DiscordErrorNotificationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DiscordWebhookClientTest {

    @Test
    void send_postsPayloadToConfiguredWebhook() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DiscordWebhookClient client = new DiscordWebhookClient(
                builder.build(),
                new DiscordErrorNotificationProperties("https://example.com/webhook")
        );
        DiscordWebhookPayload payload = new DiscordWebhookPayload("Inflace Error Bot", null, List.of());

        server.expect(requestTo("https://example.com/webhook"))
                .andExpect(method(POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.username").value("Inflace Error Bot"))
                .andRespond(withSuccess());

        client.send(payload);

        server.verify();
    }
}

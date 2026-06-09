package com.example.inflace.global.client;

import com.example.inflace.global.notification.discord.payload.DiscordWebhookPayload;
import com.example.inflace.global.properties.DiscordErrorNotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class DiscordWebhookClient {

    private final RestClient restClient;
    private final DiscordErrorNotificationProperties properties;

    public void send(DiscordWebhookPayload payload) {
        restClient.post()
                .uri(properties.webhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}

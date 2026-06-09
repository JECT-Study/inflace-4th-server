package com.example.inflace.global.notification.discord.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordWebhookEmbed(
        String title,
        String description,
        Integer color,
        List<DiscordWebhookEmbedField> fields,
        DiscordWebhookEmbedFooter footer,
        String timestamp
) {
}

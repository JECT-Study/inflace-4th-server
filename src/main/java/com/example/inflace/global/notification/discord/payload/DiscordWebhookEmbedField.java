package com.example.inflace.global.notification.discord.payload;

public record DiscordWebhookEmbedField(
        String name,
        String value,
        boolean inline
) {
}

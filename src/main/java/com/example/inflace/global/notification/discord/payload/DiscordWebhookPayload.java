package com.example.inflace.global.notification.discord.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordWebhookPayload(
        String username,
        @JsonProperty("avatar_url")
        String avatarUrl,
        List<DiscordWebhookEmbed> embeds
) {
}

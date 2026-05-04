package com.example.inflace.infra.openai;

public record OpenAiSendRequest(
        String systemMessage,
        String userMessage,
        String assistantMessage,
        OpenAiModel model
) {
}

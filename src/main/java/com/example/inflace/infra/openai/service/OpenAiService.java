package com.example.inflace.infra.openai.service;

import com.example.inflace.infra.openai.OpenAiSendRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenAiService {

    private final ChatClient openAiChatClient;

    public OpenAiService(ChatClient.Builder chatClientBuilder) {
        this.openAiChatClient = chatClientBuilder.build();
    }

    public String sendChatMessage(OpenAiSendRequest request) {
        List<Message> messages = new ArrayList<>();

        if (StringUtils.hasText(request.systemMessage())) {
            messages.add(new SystemMessage(request.systemMessage()));
        }
        if (StringUtils.hasText(request.userMessage())) {
            messages.add(new UserMessage(request.userMessage()));
        }
        if (StringUtils.hasText(request.assistantMessage())) {
            messages.add(new AssistantMessage(request.assistantMessage()));
        }

        OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                .model(request.model().modelName())
                .temperature(0.7)
                .build();

        Prompt prompt = new Prompt(messages, openAiChatOptions);

        return openAiChatClient.prompt(prompt)
                .call()
                .content();
    }
}

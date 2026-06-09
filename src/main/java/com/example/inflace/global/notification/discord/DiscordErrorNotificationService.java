package com.example.inflace.global.notification.discord;

import com.example.inflace.global.client.DiscordWebhookClient;
import com.example.inflace.global.notification.discord.payload.DiscordWebhookEmbed;
import com.example.inflace.global.notification.discord.payload.DiscordWebhookEmbedField;
import com.example.inflace.global.notification.discord.payload.DiscordWebhookEmbedFooter;
import com.example.inflace.global.notification.discord.payload.DiscordWebhookPayload;
import com.example.inflace.global.properties.DiscordErrorNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordErrorNotificationService {

    private static final String DEFAULT_USERNAME = "Inflace Error Bot";
    private static final int EMBED_COLOR = 0xE74C3C;
    private static final int MAX_FIELD_VALUE_LENGTH = 900;
    private static final String REDACTED = "[REDACTED]";
    private static final List<Pattern> SENSITIVE_VALUE_PATTERNS = List.of(
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*)([^\\s,]+)"),
            Pattern.compile("(?i)(bearer\\s+)([a-z0-9._~+/=-]+)"),
            Pattern.compile("(?i)((?:access[_-]?token|refresh[_-]?token|api[_-]?key|client[_-]?secret|password)\\s*[:=]\\s*)([^\\s,}&]+)"),
            Pattern.compile("(?i)(https://discord\\.com/api/webhooks/\\d+/)([^\\s]+)")
    );
    private final DiscordWebhookClient discordWebhookClient;
    private final DiscordErrorNotificationProperties properties;
    private final ExecutorService discordErrorNotificationExecutor;

    public void notifyAsync(
            String source,
            Throwable exception,
            HttpStatus httpStatus,
            String errorCode,
            String responseMessage
    ) {
        if (!properties.isEnabled()) {
            return;
        }

        Map<String, String> mdcSnapshot = snapshotMdc();
        try {
            CompletableFuture.runAsync(
                            () -> send(source, exception, httpStatus, errorCode, responseMessage, mdcSnapshot),
                            discordErrorNotificationExecutor
                    )
                    .exceptionally(ex -> {
                        log.warn("Discord error notification failed: {}", ex.getMessage(), ex);
                        return null;
                    });
        } catch (RuntimeException ex) {
            log.warn("Discord error notification scheduling failed: {}", ex.getMessage(), ex);
        }
    }

    private void send(
            String source,
            Throwable exception,
            HttpStatus httpStatus,
            String errorCode,
            String responseMessage,
            Map<String, String> mdcSnapshot
    ) {
        DiscordWebhookPayload payload = new DiscordWebhookPayload(
                DEFAULT_USERNAME,
                null,
                List.of(buildEmbed(source, exception, httpStatus, errorCode, responseMessage, mdcSnapshot))
        );

        discordWebhookClient.send(payload);
    }

    private DiscordWebhookEmbed buildEmbed(
            String source,
            Throwable exception,
            HttpStatus httpStatus,
            String errorCode,
            String responseMessage,
            Map<String, String> mdcSnapshot
    ) {
        String exceptionMessage = redactSensitiveValues(resolveExceptionMessage(exception, responseMessage));
        String stackTrace = truncate(redactSensitiveValues(toStackTrace(exception)), MAX_FIELD_VALUE_LENGTH);

        List<DiscordWebhookEmbedField> fields = List.of(
                new DiscordWebhookEmbedField("상태", httpStatus.value() + " " + httpStatus.getReasonPhrase(), true),
                new DiscordWebhookEmbedField("에러 코드", errorCode, true),
                new DiscordWebhookEmbedField("예외 타입", exception.getClass().getName(), false),
                new DiscordWebhookEmbedField("예외 메시지", wrapCodeBlock(truncate(exceptionMessage, MAX_FIELD_VALUE_LENGTH)), false),
                new DiscordWebhookEmbedField("요청", wrapCodeBlock(resolveRequestLine(mdcSnapshot)), false),
                new DiscordWebhookEmbedField("MDC", wrapCodeBlock(formatMdc(mdcSnapshot)), false),
                new DiscordWebhookEmbedField("스택 트레이스", wrapCodeBlock(stackTrace), false)
        );

        return new DiscordWebhookEmbed(
                "애플리케이션 오류 발생",
                truncate(source, MAX_FIELD_VALUE_LENGTH),
                EMBED_COLOR,
                fields,
                new DiscordWebhookEmbedFooter("Inflace error notification"),
                OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }

    private Map<String, String> snapshotMdc() {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        if (contextMap == null || contextMap.isEmpty()) {
            return Map.of(
                    "coId", "",
                    "clientIp", "",
                    "method", "",
                    "url", "",
                    "userId", ""
            );
        }

        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("coId", valueOrBlank(contextMap.get("coId")));
        snapshot.put("clientIp", valueOrBlank(contextMap.get("clientIp")));
        snapshot.put("method", valueOrBlank(contextMap.get("method")));
        snapshot.put("url", valueOrBlank(contextMap.get("url")));
        snapshot.put("userId", valueOrBlank(contextMap.get("userId")));
        return snapshot;
    }

    private String resolveRequestLine(Map<String, String> mdcSnapshot) {
        return valueOrBlank(mdcSnapshot.get("method")) + " " + valueOrBlank(mdcSnapshot.get("url"));
    }

    private String formatMdc(Map<String, String> mdcSnapshot) {
        return "coId=" + valueOrBlank(mdcSnapshot.get("coId")) + '\n'
                + "clientIp=" + valueOrBlank(mdcSnapshot.get("clientIp")) + '\n'
                + "method=" + valueOrBlank(mdcSnapshot.get("method")) + '\n'
                + "url=" + valueOrBlank(mdcSnapshot.get("url")) + '\n'
                + "userId=" + valueOrBlank(mdcSnapshot.get("userId"));
    }

    private String resolveExceptionMessage(Throwable exception, String responseMessage) {
        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }

        return responseMessage;
    }

    private String wrapCodeBlock(String value) {
        return "```text\n" + value + "\n```";
    }

    private String toStackTrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    private String redactSensitiveValues(String value) {
        String redacted = valueOrBlank(value);
        for (Pattern pattern : SENSITIVE_VALUE_PATTERNS) {
            redacted = pattern.matcher(redacted).replaceAll("$1" + REDACTED);
        }
        return redacted;
    }

    private String valueOrBlank(String value) {
        return value != null ? value : "";
    }
}

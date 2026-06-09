package com.example.inflace.global.notification.discord;

import com.example.inflace.global.client.DiscordWebhookClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.notification.discord.payload.DiscordWebhookPayload;
import com.example.inflace.global.properties.DiscordErrorNotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DiscordErrorNotificationServiceTest {

    private DiscordErrorNotificationService discordErrorNotificationService;
    private DiscordWebhookClient discordWebhookClient;

    @BeforeEach
    void setUp() {
        discordWebhookClient = mock(DiscordWebhookClient.class);
        DiscordErrorNotificationProperties properties = new DiscordErrorNotificationProperties("https://example.com/webhook");
        ExecutorService directExecutor = new DirectExecutorService();

        discordErrorNotificationService = new DiscordErrorNotificationService(discordWebhookClient, properties, directExecutor);
    }

    @Test
    void notifyAsync_sendsDiscordEmbedForServerError() {
        MDC.put("coId", "req-123");
        MDC.put("clientIp", "127.0.0.1");
        MDC.put("method", "GET");
        MDC.put("url", "/api/videos");
        MDC.put("userId", "42");

        discordErrorNotificationService.notifyAsync(
                "GlobalRestExceptionHandler.handleException",
                new IllegalStateException("boom"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );

        ArgumentCaptor<DiscordWebhookPayload> payloadCaptor = ArgumentCaptor.forClass(DiscordWebhookPayload.class);
        verify(discordWebhookClient).send(payloadCaptor.capture());
        DiscordWebhookPayload payload = payloadCaptor.getValue();
        assertThat(payload.username()).isEqualTo("Inflace Error Bot");
        assertThat(payload.embeds().getFirst().title()).isEqualTo("애플리케이션 오류 발생");
        assertThat(payload.embeds().getFirst().fields().get(5).value()).contains("coId=req-123");
        MDC.clear();
    }

    @Test
    void notifyAsync_sendsApiExceptionSelectedByAspect() {
        MDC.put("coId", "req-456");

        ApiException exception = new ApiException(ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST);

        discordErrorNotificationService.notifyAsync(
                "GlobalRestExceptionHandler.handleApiException",
                exception,
                ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getHttpStatus(),
                ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getErrorCode(),
                ErrorDefine.DUPLICATE_IDEMPOTENCY_REQUEST.getMessage()
        );

        verify(discordWebhookClient).send(org.mockito.ArgumentMatchers.any(DiscordWebhookPayload.class));
        MDC.clear();
    }

    @Test
    void notifyAsync_sendsWithoutApplyingNotificationPolicy() {
        ApiException exception = new ApiException(ErrorDefine.USER_NOT_FOUND);

        discordErrorNotificationService.notifyAsync(
                "GlobalRestExceptionHandler.handleApiException",
                exception,
                ErrorDefine.USER_NOT_FOUND.getHttpStatus(),
                ErrorDefine.USER_NOT_FOUND.getErrorCode(),
                ErrorDefine.USER_NOT_FOUND.getMessage()
        );

        verify(discordWebhookClient).send(org.mockito.ArgumentMatchers.any(DiscordWebhookPayload.class));
    }

    @Test
    void notifyAsync_skipsNotificationWhenWebhookUrlIsMissing() {
        DiscordErrorNotificationService service = new DiscordErrorNotificationService(
                discordWebhookClient,
                new DiscordErrorNotificationProperties(""),
                new DirectExecutorService()
        );

        service.notifyAsync(
                "GlobalRestExceptionHandler.handleException",
                new IllegalStateException("boom"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );
    }

    @Test
    void notifyAsync_redactsSensitiveValues() {
        discordErrorNotificationService.notifyAsync(
                "GlobalRestExceptionHandler.handleException",
                new IllegalStateException("access_token=secret-token"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        );

        ArgumentCaptor<DiscordWebhookPayload> payloadCaptor = ArgumentCaptor.forClass(DiscordWebhookPayload.class);
        verify(discordWebhookClient).send(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().embeds().getFirst().fields().get(3).value())
                .contains("access_token=[REDACTED]")
                .doesNotContain("secret-token");
    }

    @Test
    void notifyAsync_doesNotPropagateExecutorRejection() {
        DiscordErrorNotificationService service = new DiscordErrorNotificationService(
                discordWebhookClient,
                new DiscordErrorNotificationProperties("https://example.com/webhook"),
                new RejectingExecutorService()
        );

        assertThatCode(() -> service.notifyAsync(
                "GlobalRestExceptionHandler.handleException",
                new IllegalStateException("boom"),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "500",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()
        )).doesNotThrowAnyException();
    }

    private static class DirectExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static class RejectingExecutorService extends DirectExecutorService {
        @Override
        public void execute(Runnable command) {
            throw new RejectedExecutionException("rejected");
        }
    }
}

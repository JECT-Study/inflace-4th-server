package com.example.inflace.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.global.client.GoogleApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthTokenServiceTest {

    private static final String GOOGLE_ID = "google-user";

    @Mock
    private GoogleAccessTokenStore googleAccessTokenStore;

    @Mock
    private GoogleApiClient googleApiClient;

    private GoogleOAuthTokenService googleOAuthTokenService;

    @BeforeEach
    void setUp() {
        googleOAuthTokenService = new GoogleOAuthTokenService(googleAccessTokenStore, googleApiClient);
    }

    @Test
    void returnsStoredAccessTokenWhenStillValid() {
        when(googleAccessTokenStore.findAccessToken(GOOGLE_ID)).thenReturn("cached-access-token");

        String result = googleOAuthTokenService.executeWithRefresh(GOOGLE_ID, token -> "ok:" + token);

        assertThat(result).isEqualTo("ok:cached-access-token");
        verify(googleApiClient, never()).refreshToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void refreshesWhenAccessTokenIsMissing() {
        when(googleAccessTokenStore.findAccessToken(GOOGLE_ID)).thenReturn(null);
        when(googleAccessTokenStore.getRefreshToken(GOOGLE_ID)).thenReturn("stored-refresh-token");
        when(googleApiClient.refreshToken("stored-refresh-token"))
                .thenReturn(new GoogleTokenResponse("new-access-token", null, "Bearer", 3600L));

        String result = googleOAuthTokenService.executeWithRefresh(GOOGLE_ID, token -> "ok:" + token);

        assertThat(result).isEqualTo("ok:new-access-token");
        verify(googleAccessTokenStore).saveAccessToken(GOOGLE_ID, "new-access-token", 3600L);
    }

    @Test
    void retriesOnceAfterUnauthorizedResponse() {
        when(googleAccessTokenStore.findAccessToken(GOOGLE_ID)).thenReturn("expired-access-token");
        when(googleAccessTokenStore.getRefreshToken(GOOGLE_ID)).thenReturn("stored-refresh-token");
        when(googleApiClient.refreshToken("stored-refresh-token"))
                .thenReturn(new GoogleTokenResponse("refreshed-access-token", null, "Bearer", 3600L));

        AtomicInteger invocationCount = new AtomicInteger();

        String result = googleOAuthTokenService.executeWithRefresh(GOOGLE_ID, token -> {
            if (invocationCount.getAndIncrement() == 0) {
                throw unauthorizedException();
            }
            return "ok:" + token;
        });

        assertThat(result).isEqualTo("ok:refreshed-access-token");
        assertThat(invocationCount.get()).isEqualTo(2);
        verify(googleAccessTokenStore).saveAccessToken(GOOGLE_ID, "refreshed-access-token", 3600L);
    }

    @Test
    void throwsApiExceptionWhenRefreshTokenIsRejected() {
        when(googleAccessTokenStore.findAccessToken(GOOGLE_ID)).thenReturn(null);
        when(googleAccessTokenStore.getRefreshToken(GOOGLE_ID)).thenReturn("invalid-refresh-token");
        when(googleApiClient.refreshToken("invalid-refresh-token")).thenThrow(badRequestException());

        assertThatThrownBy(() -> googleOAuthTokenService.getAuthorizedAccessToken(GOOGLE_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getError())
                .isEqualTo(ErrorDefine.INVALID_REFRESH_TOKEN);
    }

    private HttpClientErrorException unauthorizedException() {
        return HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
    }

    private HttpClientErrorException badRequestException() {
        return HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
    }
}

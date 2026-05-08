package com.example.inflace.domain.auth.service;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.global.client.GoogleApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class GoogleOAuthTokenService {

    private final GoogleAccessTokenStore googleAccessTokenStore;
    private final GoogleApiClient googleApiClient;

    public void saveTokens(String googleId, GoogleTokenResponse tokenResponse) {
        googleAccessTokenStore.saveTokens(
                googleId,
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn()
        );
    }

    public String getAuthorizedAccessToken(String googleId) {
        String accessToken = googleAccessTokenStore.findAccessToken(googleId);
        return accessToken != null ? accessToken : refreshAccessToken(googleId);
    }

    public <T> T executeWithRefresh(String googleId, Function<String, T> request) {
        String accessToken = getAuthorizedAccessToken(googleId);
        try {
            return request.apply(accessToken);
        } catch (RestClientResponseException exception) {
            if (!isUnauthorized(exception)) {
                throw exception;
            }
        }

        return request.apply(refreshAccessToken(googleId));
    }

    private String refreshAccessToken(String googleId) {
        String refreshToken = googleAccessTokenStore.getRefreshToken(googleId);

        try {
            GoogleTokenResponse tokenResponse = googleApiClient.getTokenWithRefreshToken(refreshToken);
            googleAccessTokenStore.saveAccessToken(googleId, tokenResponse.accessToken(), tokenResponse.expiresIn());
            return tokenResponse.accessToken();
        } catch (RestClientResponseException exception) {
            if (isUnauthorized(exception) || exception.getStatusCode().value() == 400) {
                throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
            }
            throw exception;
        }
    }

    private boolean isUnauthorized(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 401;
    }
}

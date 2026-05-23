package com.example.inflace.domain.auth.local.service;

import com.example.inflace.domain.auth.local.properties.LocalLoginProperties;
import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.GoogleUserInfoResponse;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.service.GoogleOAuthTokenService;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.client.GoogleApiClient;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalOAuthUserInfoService {

    private static final String GOOGLE_PROVIDER = "google";
    private static final String YOUTUBE_PROVIDER = "youtube";

    private final GoogleApiClient googleApiClient;
    private final GoogleOAuthTokenService googleOAuthTokenService;
    private final LocalLoginProperties localLoginProperties;

    public OAuthUserInfo getUserInfo(String provider, String code) {
        String redirectUri = switch (provider) {
            case GOOGLE_PROVIDER -> localLoginProperties.googleRedirectUri();
            case YOUTUBE_PROVIDER -> localLoginProperties.youtubeRedirectUri();
            default -> throw new ApiException(ErrorDefine.AUTH_UNSUPPORTED_PROVIDER);
        };

        GoogleTokenResponse token = googleApiClient.getToken(code, redirectUri);
        GoogleUserInfoResponse userInfo = googleApiClient.getUserInfo(token.accessToken());
        googleOAuthTokenService.saveTokens(userInfo.sub(), token);

        return new OAuthUserInfo(userInfo.sub(), userInfo.name(), userInfo.email(), userInfo.picture(), Plan.FREE);
    }
}

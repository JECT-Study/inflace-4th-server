package com.example.inflace.domain.auth.application;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.GoogleUserInfoResponse;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.global.client.GoogleApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("google")
@RequiredArgsConstructor
public class GoogleLoginStrategy implements OAuthLoginStrategy {

    private final GoogleApiClient googleApiClient;
    private final GoogleAccessTokenStore googleAccessTokenStore;

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        GoogleTokenResponse token = googleApiClient.getToken(code);
        GoogleUserInfoResponse userInfo = googleApiClient.getUserInfo(token.accessToken());
        googleAccessTokenStore.save(userInfo.sub(), token.accessToken());  // TODO : 인메모리에 저장, 향후 REDIS로 옮기기

        return new OAuthUserInfo(userInfo.sub(), userInfo.name(), userInfo.email(), userInfo.picture());
    }
}

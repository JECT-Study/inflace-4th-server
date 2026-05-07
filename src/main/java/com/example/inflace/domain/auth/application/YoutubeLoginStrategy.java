package com.example.inflace.domain.auth.application;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.GoogleUserInfoResponse;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.client.GoogleApiClient;
import com.example.inflace.global.properties.YoutubeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("youtube")
@RequiredArgsConstructor
@Slf4j
public class YoutubeLoginStrategy implements OAuthLoginStrategy {

    private final GoogleApiClient googleApiClient;
    private final GoogleAccessTokenStore googleAccessTokenStore;
    private final YoutubeProperties youtubeProperties;

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        log.info("youtube login step=exchange-code");
        GoogleTokenResponse token = googleApiClient.getToken(code, youtubeProperties.oauth().redirectUri());

        log.info("youtube login step=fetch-user-info");
        GoogleUserInfoResponse userInfo = googleApiClient.getUserInfo(token.accessToken());

        log.info("youtube login step=save-google-access-token");
        googleAccessTokenStore.save(userInfo.sub(), token.accessToken(), token.expiresIn());

        return new OAuthUserInfo(userInfo.sub(), userInfo.name(), userInfo.email(), userInfo.picture(), Plan.FREE);
    }
}

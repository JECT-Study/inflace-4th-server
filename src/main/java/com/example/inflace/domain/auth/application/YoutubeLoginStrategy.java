package com.example.inflace.domain.auth.application;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.GoogleUserInfoResponse;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.user.domain.enums.Plan;
import com.example.inflace.global.client.GoogleApiClient;
import com.example.inflace.global.properties.YoutubeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("youtube")
@RequiredArgsConstructor
public class YoutubeLoginStrategy implements OAuthLoginStrategy {

    private final GoogleApiClient googleApiClient;
    private final YoutubeProperties youtubeProperties;

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        GoogleTokenResponse token = googleApiClient.getToken(code, youtubeProperties.oauth().redirectUri());
        GoogleUserInfoResponse userInfo = googleApiClient.getUserInfo(token.accessToken());

        return new OAuthUserInfo(userInfo.sub(), userInfo.name(), userInfo.email(), userInfo.picture(), Plan.FREE, token.accessToken());
    }
}

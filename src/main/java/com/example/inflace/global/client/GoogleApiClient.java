package com.example.inflace.global.client;

import com.example.inflace.domain.auth.presentation.dto.GoogleTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.GoogleUserInfoResponse;
import com.example.inflace.global.properties.GoogleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class GoogleApiClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final GoogleProperties googleProperties;

    public GoogleTokenResponse getToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleProperties.clientId());
        params.add("client_secret", googleProperties.clientSecret());
        params.add("redirect_uri", googleProperties.redirectUri());
        params.add("grant_type", "authorization_code");

        return restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        return restClient.get()
                .uri(USER_INFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfoResponse.class);
    }
}

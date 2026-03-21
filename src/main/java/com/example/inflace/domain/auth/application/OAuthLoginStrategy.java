package com.example.inflace.domain.auth.application;

import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;

public interface OAuthLoginStrategy {

    OAuthUserInfo getUserInfo(String code);
}

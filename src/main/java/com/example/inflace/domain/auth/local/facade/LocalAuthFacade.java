package com.example.inflace.domain.auth.local.facade;

import com.example.inflace.domain.auth.local.service.LocalOAuthUserInfoService;
import com.example.inflace.domain.auth.presentation.dto.AuthFacadeLoginResponse;
import com.example.inflace.domain.auth.presentation.dto.LoginRequest;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.domain.auth.presentation.dto.UserDetailsResponse;
import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAuthFacade {

    private final LocalOAuthUserInfoService localOAuthUserInfoService;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final AuthTokenRedisService authTokenRedisService;

    public AuthFacadeLoginResponse login(LoginRequest request) {
        OAuthUserInfo userInfo = localOAuthUserInfoService.getUserInfo(request.provider(), request.code());

        UserRegistrationResult result = userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture(),
                userInfo.plan()
        );

        UserDetailsResponse userDetails = userService.getUserDetails(result.userId());

        String accessToken = jwtProvider.createAccessToken(userDetails.id(), userDetails.userRoles());
        String refreshToken = jwtProvider.createRefreshToken(userDetails.id());
        authTokenRedisService.saveRefreshToken(
                result.userId(),
                refreshToken,
                jwtProvider.getRefreshTokenExpirationMillis()
        );

        return AuthFacadeLoginResponse.of(
                new TokenData(accessToken, refreshToken),
                userDetails,
                userService.getUserChannelDetails(userDetails.id())
        );
    }
}

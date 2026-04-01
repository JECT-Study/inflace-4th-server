package com.example.inflace.domain.auth.facade;

import com.example.inflace.domain.auth.application.OAuthStrategyRouter;
import com.example.inflace.domain.auth.presentation.dto.AuthResponse;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.global.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final OAuthStrategyRouter oAuthStrategyRouter;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    public AuthResponse login(String provider, String code) {
        OAuthUserInfo userInfo = oAuthStrategyRouter.getStrategy(provider).getUserInfo(code);

        userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture()
        );

        String accessToken = jwtProvider.createAccessToken(userInfo.sub());
        String refreshToken = jwtProvider.createRefreshToken(userInfo.sub());

        return new AuthResponse(accessToken, refreshToken);
    }
}

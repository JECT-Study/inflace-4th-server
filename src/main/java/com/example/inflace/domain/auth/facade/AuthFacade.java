package com.example.inflace.domain.auth.facade;

import com.example.inflace.domain.auth.application.OAuthStrategyRouter;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.domain.enums.UserRole;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.security.jwt.JwtProvider;
import com.example.inflace.global.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final OAuthStrategyRouter oAuthStrategyRouter;
    private final UserService userService;
    private final UserReadRepository userReadRepository;
    private final JwtProvider jwtProvider;
    private final AuthTokenRedisService authTokenRedisService;

    public TokenData login(String provider, String code) {
        OAuthUserInfo userInfo = oAuthStrategyRouter.getStrategy(provider).getUserInfo(code);

        UserRegistrationResult result = userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture(),
                userInfo.plan()
        );
        UserRole userType = userReadRepository.findUserRoleByUserId(result.userId()).orElse(null);

        String accessToken = jwtProvider.createAccessToken(
                result.userId(),
                userInfo.picture(),
                result.isNewUser(),
                userInfo.plan(),
                userType
        );
        String refreshToken = jwtProvider.createRefreshToken(result.userId());

        authTokenRedisService.saveRefreshToken(result.userId(), refreshToken, jwtProvider.getRefreshTokenExpirationMillis());

        return new TokenData(accessToken, refreshToken);
    }

    public void logout(String accessToken) {
        var userId = SecurityUtils.getAuthenticatedUserId();
        authTokenRedisService.deleteRefreshToken(userId);
        if (accessToken != null && !accessToken.isBlank() && jwtProvider.isValid(accessToken)) {
            long remainingMillis = jwtProvider.getRemainingExpirationMillis(accessToken);
            if (remainingMillis > 0) {
                authTokenRedisService.saveLogoutAccessToken(accessToken, remainingMillis);
            }
        }
        SecurityUtils.clear();
    }

    public TokenData reissue(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtProvider.getUserId(refreshToken);

        if (!authTokenRedisService.isValidRefreshToken(userId, refreshToken)) {
            throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
        }

        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
        UserRole userType = userReadRepository.findUserRoleByUserId(userId).orElse(null);

        String newAccessToken = jwtProvider.createAccessToken(
                userId,
                user.getProfileImage(),
                false,
                user.getPlan(),
                userType
        );
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        authTokenRedisService.saveRefreshToken(userId, newRefreshToken, jwtProvider.getRefreshTokenExpirationMillis());

        return new TokenData(newAccessToken, newRefreshToken);
    }
}

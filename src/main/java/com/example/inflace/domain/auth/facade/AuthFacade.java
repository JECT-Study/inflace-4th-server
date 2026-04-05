package com.example.inflace.domain.auth.facade;

import com.example.inflace.domain.auth.application.OAuthStrategyRouter;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.domain.auth.util.RefreshTokenStore;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.global.config.JwtProvider;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final OAuthStrategyRouter oAuthStrategyRouter;
    private final UserService userService;
    private final UserReadRepository userReadRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    public TokenData login(String provider, String code) {
        OAuthUserInfo userInfo = oAuthStrategyRouter.getStrategy(provider).getUserInfo(code);

        UserRegistrationResult result = userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture(),
                userInfo.plan()
        );

        String accessToken = jwtProvider.createAccessToken(
                result.userId(),
                userInfo.picture(),
                result.isNewUser(),
                userInfo.plan()
        );
        String refreshToken = jwtProvider.createRefreshToken(result.userId());

        refreshTokenStore.save(result.userId(), refreshToken);

        return new TokenData(accessToken, refreshToken);
    }

    public void logout(long userId) {
        refreshTokenStore.deleteByUserId(userId);
    }

    public TokenData reissue(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isValid(refreshToken)) {
            throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
        }

        long userId = jwtProvider.getUserId(refreshToken);

        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            throw new ApiException(ErrorDefine.INVALID_REFRESH_TOKEN);
        }

        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));


        String newAccessToken = jwtProvider.createAccessToken(userId, user.getProfileImage(), false, user.getPlan());
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenStore.save(userId, newRefreshToken);

        return new TokenData(newAccessToken, newRefreshToken);
    }
}

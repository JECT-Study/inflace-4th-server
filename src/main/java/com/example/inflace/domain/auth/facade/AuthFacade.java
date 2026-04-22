package com.example.inflace.domain.auth.facade;

import com.example.inflace.domain.auth.application.OAuthStrategyRouter;
import com.example.inflace.domain.auth.presentation.dto.*;
import com.example.inflace.domain.auth.service.AuthTokenRedisService;
import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.UserChannelDetailsResponse;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.user.application.UserService;
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
    private final ChannelRepository channelRepository;

    public AuthFacadeLoginResponse login(LoginRequest request) {
        OAuthUserInfo userInfo = oAuthStrategyRouter.getStrategy(request.provider()).getUserInfo(request.code());

        UserRegistrationResult result = userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture(),
                userInfo.plan()
        );

        UserDetailsResponse userDetails = userService.getUserDetails(result.userId());

        Channel channel = channelRepository.findByUser_Id(userDetails.id()).orElse(null);

        String accessToken = jwtProvider.createAccessToken(userDetails.id(), userDetails.userRoles());
        String refreshToken = jwtProvider.createRefreshToken(userDetails.id());

        authTokenRedisService.saveRefreshToken(result.userId(), refreshToken, jwtProvider.getRefreshTokenExpirationMillis());

        return AuthFacadeLoginResponse.of(
                new TokenData(accessToken, refreshToken),
                userDetails,
                channel != null ? UserChannelDetailsResponse.from(channel) : null
        );
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

        UserDetailsResponse userDetails = userService.getUserDetails(userId);

        String newAccessToken = jwtProvider.createAccessToken(userDetails.id(), userDetails.userRoles());
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        authTokenRedisService.saveRefreshToken(userId, newRefreshToken, jwtProvider.getRefreshTokenExpirationMillis());

        return new TokenData(newAccessToken, newRefreshToken);
    }
}

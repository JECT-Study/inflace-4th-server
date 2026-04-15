package com.example.inflace.domain.auth.facade;

import com.example.inflace.domain.auth.application.OAuthStrategyRouter;
import com.example.inflace.domain.auth.presentation.dto.OAuthUserInfo;
import com.example.inflace.domain.auth.presentation.dto.TokenData;
import com.example.inflace.domain.auth.util.GoogleAccessTokenStore;
import com.example.inflace.domain.auth.util.RefreshTokenStore;
import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.user.application.UserService;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.user.infra.UserRegistrationResult;
import com.example.inflace.global.config.JwtProvider;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final OAuthStrategyRouter oAuthStrategyRouter;
    private final UserService userService;
    private final UserReadRepository userReadRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final GoogleAccessTokenStore googleAccessTokenStore;
    private final ChannelRepository channelRepository;

    public TokenData login(String provider, String code) {
        OAuthUserInfo userInfo = oAuthStrategyRouter.getStrategy(provider).getUserInfo(code);

        UserRegistrationResult result = userService.registerIfNotExists(
                userInfo.sub(),
                userInfo.name(),
                userInfo.email(),
                userInfo.picture(),
                userInfo.plan()
        );

        User user = userReadRepository.findById(result.userId())
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        Optional<Channel> channelOpt = channelRepository.findByUser_Id(result.userId());
        String accessToken = jwtProvider.createAccessToken(
                result.userId(),
                userInfo.picture(),
                userInfo.plan(),
                channelOpt.map(Channel::getName).orElse(null),
                channelOpt.map(Channel::getProfileImage).orElse(null),
                user.isOnboardingCompleted()
        );
        String refreshToken = jwtProvider.createRefreshToken(result.userId());

        refreshTokenStore.save(result.userId(), refreshToken);

        if (userInfo.accessToken() != null) {
            googleAccessTokenStore.save(result.userId(), userInfo.accessToken());
        }

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


        Optional<Channel> channelOpt = channelRepository.findByUser_Id(userId);
        String newAccessToken = jwtProvider.createAccessToken(
                userId,
                user.getProfileImage(),
                user.getPlan(),
                channelOpt.map(Channel::getName).orElse(null),
                channelOpt.map(Channel::getProfileImage).orElse(null),
                user.isOnboardingCompleted()
        );
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenStore.save(userId, newRefreshToken);

        return new TokenData(newAccessToken, newRefreshToken);
    }
}

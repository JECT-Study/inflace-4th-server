package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.dto.ConnectChannelTarget;
import com.example.inflace.domain.channel.dto.RefreshChannelTarget;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChannelSyncQueryService {

    private final UserReadRepository userReadRepository;
    private final ChannelRepository channelRepository;

    @Transactional(readOnly = true)
    public ConnectChannelTarget getConnectTarget(UUID userId) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        return new ConnectChannelTarget(user.getId(), user.getProviderId());
    }

    @Transactional(readOnly = true)
    public RefreshChannelTarget getRefreshTarget(UUID userId, Long channelId) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        if (!channel.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorDefine.AUTH_FORBIDDEN);
        }

        return new RefreshChannelTarget(
                user.getId(),
                user.getProviderId(),
                channel.getId(),
                channel.getYoutubeChannelId()
        );
    }

}

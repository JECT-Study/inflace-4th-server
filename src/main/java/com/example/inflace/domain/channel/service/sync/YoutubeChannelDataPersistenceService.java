package com.example.inflace.domain.channel.service.sync;

import com.example.inflace.domain.channel.dto.ChannelDataSyncResult;
import com.example.inflace.domain.channel.dto.response.YoutubeDataChannelResponse;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse;
import com.example.inflace.domain.video.dto.YoutubeDataVideoResponse.Item;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeChannelDataPersistenceService {

    private final UserReadRepository userReadRepository;
    private final YoutubeChannelDataSyncService youtubeChannelDataSyncService;

    @Transactional
    public ChannelDataSyncResult persistChannelData(
            UUID userId,
            YoutubeDataChannelResponse.Item channelItem,
            List<YoutubeDataVideoResponse.Item> videoItems
    ) {
        User user = userReadRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorDefine.USER_NOT_FOUND));

        return youtubeChannelDataSyncService.synchronizeChannel(
                user,
                channelItem,
                videoItems
        );
    }
}

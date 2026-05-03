package com.example.inflace.domain.channel.service;

import com.example.inflace.domain.channel.domain.Channel;
import com.example.inflace.domain.channel.domain.ChannelBookmark;
import com.example.inflace.domain.channel.dto.request.InfluencerSearchCondition;
import com.example.inflace.domain.channel.dto.request.InfluencerSortCriteria;
import com.example.inflace.domain.channel.dto.response.GetInfluencerSearchResponse;
import com.example.inflace.domain.channel.dto.response.GetInfluencerBookmarksResponse;
import com.example.inflace.domain.channel.repository.ChannelBookmarkRepository;
import com.example.inflace.domain.channel.repository.ChannelRepository;
import com.example.inflace.domain.channel.repository.querydsl.CustomInfluencerQueryRepository;
import com.example.inflace.domain.channel.repository.querydsl.InfluencerCursorCodec;
import com.example.inflace.domain.user.domain.entity.User;
import com.example.inflace.domain.user.infra.UserReadRepository;
import com.example.inflace.global.annotation.ReadOnlyTransactional;
import com.example.inflace.global.enums.SortOrder;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InfluencerService {

    private final CustomInfluencerQueryRepository influencerQueryRepository;
    private final ChannelRepository channelRepository;
    private final ChannelBookmarkRepository channelBookmarkRepository;
    private final UserReadRepository userReadRepository;
    private final InfluencerCursorCodec influencerCursorCodec;

    @ReadOnlyTransactional
    public CursorSliceResponse<GetInfluencerSearchResponse> getInfluencersWithSearchCondition(
            InfluencerSearchCondition searchCondition,
            UUID userId
    ) {
        InfluencerSortCriteria sortCriteria = searchCondition.sortCriteriaEnum();
        SortOrder sortOrder = searchCondition.sortOrder();

        InfluencerCursorCodec.DecodedInfluencerCursor cursor = influencerCursorCodec.decodeOrNull(
                searchCondition.cursor(),
                sortCriteria,
                sortOrder
        );

        Slice<GetInfluencerSearchResponse> slice = influencerQueryRepository.getInfluencersWithSearchCondition(
                searchCondition,
                userId,
                cursor
        );

        return CursorSliceResponse.from(
                slice,
                sortCriteria.value(),
                sortOrder.name(),
                buildNextCursor(slice, sortCriteria, sortOrder)
        );
    }

    @Transactional
    public void createChannelBookmark(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        User user = userReadRepository.getReferenceById(userId);

        ChannelBookmark channelBookmark = ChannelBookmark.of(channel, user);
        channelBookmarkRepository.save(channelBookmark);
    }

    @Transactional
    public void deleteChannelBookmark(Long channelId, UUID userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorDefine.CHANNEL_NOT_FOUND));

        channelBookmarkRepository.deleteByChannelAndUser(channel, userReadRepository.getReferenceById(userId));
    }

    @ReadOnlyTransactional
    public GetInfluencerBookmarksResponse getInfluencerBookmarks(UUID userId) {
        return new GetInfluencerBookmarksResponse(
                channelBookmarkRepository.findByUserId(userId).stream()
                        .map(ChannelBookmark::getChannel)
                        .map(Channel::getId)
                        .toList()
        );
    }

    private String buildNextCursor(
            Slice<GetInfluencerSearchResponse> slice,
            InfluencerSortCriteria sortCriteria,
            SortOrder sortOrder
    ) {
        if (!slice.hasNext() || slice.isEmpty()) {
            return null;
        }

        GetInfluencerSearchResponse last = slice.getContent().get(slice.getNumberOfElements() - 1);

        return switch (sortCriteria) {
            case SUBSCRIBER -> influencerCursorCodec.encode(
                    sortCriteria,
                    sortOrder,
                    last.subscriberCount(),
                    last.channelId()
            );
            case ENGAGEMENT_RATE -> influencerCursorCodec.encode(
                    sortCriteria,
                    sortOrder,
                    last.averageEngagementRate(),
                    last.channelId()
            );
        };
    }
}

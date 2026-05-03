package com.example.inflace.domain.channel.repository.querydsl;

import com.example.inflace.domain.channel.dto.request.InfluencerSortCriteria;
import com.example.inflace.global.enums.SortOrder;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InfluencerCursorCodec {

    public String encode(
            InfluencerSortCriteria sortCriteria,
            SortOrder sortOrder,
            Object sortValue,
            Long channelId
    ) {
        String raw = sortCriteria.value() + "|"
                + sortOrder.name() + "|"
                + sortValue + "|"
                + channelId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public DecodedInfluencerCursor decodeOrNull(
            String cursor,
            InfluencerSortCriteria expectedSortCriteria,
            SortOrder expectedSortOrder
    ) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] tokens = decoded.split("\\|", -1);

            if (tokens.length != 4) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }

            InfluencerSortCriteria actualSortCriteria = InfluencerSortCriteria.from(tokens[0]);
            SortOrder actualSortOrder = SortOrder.valueOf(tokens[1]);

            if (actualSortCriteria != expectedSortCriteria || actualSortOrder != expectedSortOrder) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }

            return new DecodedInfluencerCursor(
                    actualSortCriteria,
                    actualSortOrder,
                    tokens[2],
                    Long.parseLong(tokens[3])
            );
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    public record DecodedInfluencerCursor(
            InfluencerSortCriteria sortCriteria,
            SortOrder sortOrder,
            String rawSortValue,
            Long channelId
    ) {
        public Double engagementRate() {
            return Double.parseDouble(rawSortValue);
        }

        public Long subscriberCount() {
            return Long.parseLong(rawSortValue);
        }
    }
}

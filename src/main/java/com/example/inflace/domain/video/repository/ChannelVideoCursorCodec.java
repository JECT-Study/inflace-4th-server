package com.example.inflace.domain.video.repository;

import com.example.inflace.domain.channel.dto.enums.ChannelVideoSort;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ChannelVideoCursorCodec {

    public String encode(ChannelVideoSort sort, Object sortValue, Long videoId) {
        String raw = sort.name() + "|" + formatSortValue(sort, sortValue) + "|" + videoId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public DecodedChannelVideoCursor decode(String cursor, ChannelVideoSort expectedSort) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] tokens = decoded.split("\\|", -1);

            if (tokens.length != 3) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }

            ChannelVideoSort actualSort = ChannelVideoSort.valueOf(tokens[0]);
            if (actualSort != expectedSort) {
                throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
            }

            return new DecodedChannelVideoCursor(actualSort, tokens[1], Long.parseLong(tokens[2]));
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorDefine.INVALID_ARGUMENT);
        }
    }

    private String formatSortValue(ChannelVideoSort sort, Object sortValue) {
        if (sort == ChannelVideoSort.LATEST) {
            return ((LocalDateTime) sortValue).toString();
        }
        return String.valueOf(sortValue);
    }

    public record DecodedChannelVideoCursor(
            ChannelVideoSort sort,
            String rawSortValue,
            Long videoId
    ) {
        public LocalDateTime publishedAt() {
            return LocalDateTime.parse(rawSortValue);
        }

        public Long longValue() {
            return Long.parseLong(rawSortValue);
        }

        public Double doubleValue() {
            return Double.parseDouble(rawSortValue);
        }
    }
}

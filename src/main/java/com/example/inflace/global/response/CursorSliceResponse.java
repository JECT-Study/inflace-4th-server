package com.example.inflace.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Slice;
import org.springframework.util.StringUtils;

public record CursorSliceResponse<T>(
        @Schema(description = "현재 페이지 데이터 목록")
        List<T> content,

        @Schema(description = "페이지 정보")
        PageInfo pageInfo,

        @Schema(description = "정렬 정보")
        CustomSort sort
) {
    public static <T> CursorSliceResponse<T> from(
            Slice<T> slice,
            String sortCriteria,
            String sortOrder,
            String nextCursor
    ) {
        boolean sorted = slice.getSort().isSorted()
                || (StringUtils.hasText(sortCriteria) && StringUtils.hasText(sortOrder));

        return new CursorSliceResponse<>(
                slice.getContent(),
                new PageInfo(
                        slice.getSize(),
                        slice.getNumberOfElements(),
                        nextCursor,
                        slice.hasNext()
                ),
                CustomSort.of(sorted, sortCriteria, sortOrder)
        );
    }

    public record PageInfo(
            @Schema(description = "요청 페이지 크기", example = "9")
            int size,

            @Schema(description = "현재 페이지 데이터 개수", example = "9")
            int numberOfElements,

            @Schema(description = "다음 페이지 조회에 사용할 cursor", example = "c3Vic2NyaWJlcnxERVNDfDUxNDAwfDIzMQ", nullable = true)
            String nextCursor,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean hasNext
    ) {
    }
}

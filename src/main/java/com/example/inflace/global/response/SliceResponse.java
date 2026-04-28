package com.example.inflace.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;
import java.util.List;

public record SliceResponse<T>(
        @Schema(description = "현재 페이지 데이터 목록")
        List<T> content,

        @Schema(description = "요청 페이지 크기", example = "9")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "현재 페이지 데이터 개수", example = "9")
        int numberOfElements,

        @Schema(description = "현재 페이지가 비어있는지 여부", example = "false")
        boolean empty,

        @Schema(description = "정렬 정보")
        CustomSort sort
) {
    public static <T> SliceResponse<T> from(
            Slice<T> slice,
            String sortCriteria,
            String sortOrder
    ) {
        boolean sorted = slice.getSort().isSorted()
                || (sortCriteria != null && !sortCriteria.isBlank() && sortOrder != null && !sortOrder.isBlank());

        return new SliceResponse<>(
                slice.getContent(),
                slice.getSize(),
                slice.hasNext(),
                slice.getNumberOfElements(),
                slice.isEmpty(),
                CustomSort.of(sorted, sortCriteria, sortOrder)
        );
    }
}

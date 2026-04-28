package com.example.inflace.global.response;

import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> content,
        int pageSize,
        boolean hasNext,
        int numberOfElements,
        boolean empty,
        CustomSort sort
) {
    public static <T> SliceResponse<T> from(
            Slice<T> slice,
            String sortCriteria,
            String sortOrder
    ) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getSize(),
                slice.hasNext(),
                slice.getNumberOfElements(),
                slice.isEmpty(),
                CustomSort.of(slice.getSort().isSorted(), sortCriteria, sortOrder)
        );
    }
}

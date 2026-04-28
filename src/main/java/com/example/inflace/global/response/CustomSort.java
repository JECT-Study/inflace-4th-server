package com.example.inflace.global.response;

public record CustomSort(
        boolean sorted,
        String sortCriteria,
        String sortOrder
) {
    public static CustomSort of(boolean sorted, String sortCriteria, String sortOrder) {
        return new CustomSort(
                sorted,
                sortCriteria,
                sortOrder
        );
    }
}

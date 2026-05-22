package com.example.inflace.domain.user.presentation;

public record ProfileImageUploadUrlResponse(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        long expiresInSeconds
) {
}

package com.example.inflace.infra.aws.s3;

import com.example.inflace.domain.user.presentation.ProfileImageUploadUrlResponse;
import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.ErrorDefine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageStorageService {
    private static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long PRESIGNED_URL_EXPIRES_SECONDS = 300;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public ProfileImageUploadUrlResponse createProfileImageUploadUrl(UUID userId, String contentType, long fileSize) {
        validateProfileImageRequest(contentType, fileSize);
        validateStorageProperties();

        String objectKey = buildProfileImageKey(userId, contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(PRESIGNED_URL_EXPIRES_SECONDS))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new ProfileImageUploadUrlResponse(
                presignedRequest.url().toString(),
                objectKey,
                buildPublicUrl(objectKey),
                PRESIGNED_URL_EXPIRES_SECONDS
        );
    }

    public String confirmProfileImageUpload(UUID userId, String objectKey) {
        validateStorageProperties();
        validateProfileImageObjectKey(userId, objectKey);

        try {
            HeadObjectResponse object = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(objectKey)
                    .build());

            validateUploadedObject(object);
            return buildPublicUrl(objectKey);
        } catch (S3Exception e) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        } catch (SdkException e) {
            throw new ApiException(ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateStorageProperties() {
        if (!StringUtils.hasText(properties.bucket())) {
            throw new ApiException(ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED);
        }

        if (!StringUtils.hasText(properties.publicBaseUrl()) && !StringUtils.hasText(properties.region())) {
            throw new ApiException(ErrorDefine.PROFILE_IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateProfileImageRequest(String contentType, long fileSize) {
        if (fileSize <= 0 || fileSize > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        }

        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType)) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        }
    }

    private void validateProfileImageObjectKey(UUID userId, String objectKey) {
        String prefix = StringUtils.hasText(properties.profileImagePrefix())
                ? StringUtils.trimTrailingCharacter(properties.profileImagePrefix(), '/')
                : "users";
        String expectedPrefix = prefix + "/" + userId + "/profile/";

        if (!StringUtils.hasText(objectKey) || !objectKey.startsWith(expectedPrefix)) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        }
    }

    private void validateUploadedObject(HeadObjectResponse object) {
        if (object.contentLength() <= 0 || object.contentLength() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        }

        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(object.contentType())) {
            throw new ApiException(ErrorDefine.INVALID_PROFILE_IMAGE);
        }
    }

    private String buildProfileImageKey(UUID userId, String contentType) {
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String prefix = StringUtils.hasText(properties.profileImagePrefix())
                ? StringUtils.trimTrailingCharacter(properties.profileImagePrefix(), '/')
                : "users";

        return prefix + "/" + userId + "/profile/" + UUID.randomUUID() + "." + extension;
    }

    private String buildPublicUrl(String objectKey) {
        return properties.profileImagePublicBaseUrl() + "/" + objectKey;
    }
}

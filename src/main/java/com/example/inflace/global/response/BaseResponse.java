package com.example.inflace.global.response;

import com.example.inflace.global.exception.ApiException;
import com.example.inflace.global.exception.InvalidateArgumentExceptionDTO;
import com.example.inflace.global.exception.JSONConvertExceptionDTO;
import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Getter
@Builder
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean isSuccess;
    private final T responseDto;
    private ExceptionResponse error;

    public BaseResponse(@Nullable T responseDto) {
        this.isSuccess = true;
        this.responseDto = responseDto;
    }

    public static ResponseEntity<?> toResponseEntity(ApiException e) {
        return ResponseEntity.status(e.getError().getHttpStatus())
                .body(
                        BaseResponse.builder()
                                .isSuccess(false)
                                .responseDto(null)
                                .error(new ExceptionResponse(e.getError()))
                                .build());
    }

    public static ResponseEntity<Object> toResponseEntity(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        BaseResponse.builder()
                                .isSuccess(false)
                                .responseDto(null)
                                .error(new InvalidateArgumentExceptionDTO(e))
                                .build());
    }

    public static ResponseEntity<Object> toResponseEntity(HttpMessageConversionException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        BaseResponse.builder()
                                .isSuccess(false)
                                .responseDto(null)
                                .error(new JSONConvertExceptionDTO(e))
                                .build());
    }

    public static ResponseEntity<Object> toResponseEntity(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        BaseResponse.builder()
                                .isSuccess(false)
                                .responseDto(null)
                                .error(new ExceptionResponse(e))
                                .build());
    }
}
package com.example.inflace.global.controller;

import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "health check", description = "헬스 체크용 API")
public interface HealthCheckApi {

    @Operation(
            summary = "헬스 체크",
            description = "서버의 상태를 체크합니다. " +
                    "헤더에 인증을 넣으면 예외를 반환합니다."
    )
    @ApiErrorDefines(ErrorDefine.INVALID_HEADER_ERROR)
    public ResponseEntity<Void> healthCheck(String authorization);
}

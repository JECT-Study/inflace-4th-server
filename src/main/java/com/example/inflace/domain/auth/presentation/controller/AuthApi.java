package com.example.inflace.domain.auth.presentation.controller;

import com.example.inflace.domain.auth.presentation.dto.AccessTokenResponse;
import com.example.inflace.domain.auth.presentation.dto.LoginRequest;
import com.example.inflace.domain.auth.presentation.dto.LoginResponse;
import com.example.inflace.global.exception.ApiErrorDefines;
import com.example.inflace.global.exception.ErrorDefine;
import com.example.inflace.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "사용자 인증 API")
public interface AuthApi {

    @Operation(
            summary = "소셜 로그인",
            description = "소셜 로그인 제공자(google 등)의 인가코드를 전달받아 사용자를 인증하고, Access Token과 Refresh Token을 발급합니다."
    )
    @ApiErrorDefines(ErrorDefine.AUTH_UNSUPPORTED_PROVIDER)
    ResponseEntity<BaseResponse<LoginResponse>> login(LoginRequest request);

    @Operation(
            summary = "로그아웃",
            description = "현재 사용자의 Refresh Token을 무효화하고 쿠키를 삭제합니다."
    )
    @ApiErrorDefines(ErrorDefine.AUTH_FORBIDDEN)
    ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "쿠키의 Refresh Token을 검증하여 새로운 Access Token과 Refresh Token을 발급합니다."
    )
    @ApiErrorDefines({ErrorDefine.INVALID_REFRESH_TOKEN, ErrorDefine.USER_NOT_FOUND, ErrorDefine.AUTH_FORBIDDEN})
    ResponseEntity<BaseResponse<AccessTokenResponse>> reissue(
            HttpServletRequest request);
}

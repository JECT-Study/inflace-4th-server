package com.example.inflace.global.response;

import com.example.inflace.global.exception.ErrorDefine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExceptionResponse {
    private final String code;
    private final String message;

    public ExceptionResponse(ErrorDefine errorDefine) {
        this.code = errorDefine.getErrorCode();
        this.message = errorDefine.getMessage();
    }
}

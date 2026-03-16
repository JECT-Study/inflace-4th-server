package com.example.inflace.global.response;

import com.example.inflace.global.exception.ErrorDefine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

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

    public ExceptionResponse(Exception e) {
        this.code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.message = e.getMessage();
    }
}